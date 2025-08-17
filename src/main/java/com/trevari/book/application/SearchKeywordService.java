package com.trevari.book.application;

import com.trevari.book.domain.SearchKeyword;
import com.trevari.book.domain.SearchKeywordRepository;
import com.trevari.book.dto.PopularKeywordDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * 검색 키워드 관리를 담당하는 서비스 클래스
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SearchKeywordService {

    private final SearchKeywordRepository searchKeywordRepository;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    private static final String POPULAR_KEYWORDS_KEY = "popular_keywords";

    /**
     * 검색 키워드 사용 기록
     *
     * @param keyword 검색된 키워드
     */
    @Transactional(propagation = REQUIRES_NEW)
    @CacheEvict(value = "popularKeywords", allEntries = true)
    public void recordSearchKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        String normalizedKeyword = keyword.trim().toLowerCase();
        log.debug("Recording search keyword: {}", normalizedKeyword);

        //TODO Redis로 전환해서 동시성 문제 해결해야 할 듯 
        searchKeywordRepository.findByKeyword(normalizedKeyword)
                .ifPresentOrElse(
                        existingKeyword -> {
                            //있으면 카운트 + 1
                            searchKeywordRepository.incrementSearchCount(normalizedKeyword);
                        }, () -> {
                            //없으면 새로 생성
                            SearchKeyword newKeyword = SearchKeyword.builder()
                                    .keyword(normalizedKeyword)
                                    .build();
                            searchKeywordRepository.saveSearchKeyword(newKeyword);
                        }
                );
    }

    /**
     * 인기 검색 키워드 조회 (상위 10개) - MySQL 기반
     *
     * @return 검색 횟수 기준 상위 10개 키워드 목록
     */
    @Cacheable(value = "popularKeywords", key = "'top10'")
    public List<SearchKeyword> getTopSearchKeywords() {
        log.debug("Retrieving top search keywords from MySQL");

        List<SearchKeyword> topKeywords = searchKeywordRepository.findTop10ByOrderBySearchCountDesc();

        log.info("Retrieved {} top search keywords from MySQL", topKeywords.size());
        return topKeywords;
    }

    // ======== Redis 기반 개선된 메서드들 ========

    /**
     * Redis SortedSet을 사용한 검색 키워드 기록 (동시성 문제 해결)
     *
     * @param keyword 검색된 키워드
     */
    @Transactional(propagation = REQUIRES_NEW)
    public void recordSearchKeywordWithRedis(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }

        if (redisTemplate == null) {
            log.warn("Redis not available, falling back to MySQL method");
            recordSearchKeyword(keyword);
            return;
        }

        String normalizedKeyword = keyword.trim().toLowerCase();
        log.debug("Recording search keyword with Redis: {}", normalizedKeyword);

        try {
            // Redis SortedSet ZINCRBY - 원자적 연산으로 동시성 문제 해결
            redisTemplate.opsForZSet().incrementScore(POPULAR_KEYWORDS_KEY, normalizedKeyword, 1.0);
            log.debug("Successfully recorded keyword '{}' in Redis", normalizedKeyword);

        } catch (Exception e) {
            log.error("Failed to record keyword '{}' in Redis, falling back to MySQL", normalizedKeyword, e);
            recordSearchKeyword(keyword);
        }
    }

    /**
     * Redis SortedSet에서 인기 검색 키워드 조회 (실시간, O(log N + M) 성능)
     *
     * @param count 조회할 키워드 개수 (기본 10개)
     * @return 검색 횟수 기준 상위 키워드 목록
     */
    public List<PopularKeywordDto> getTopSearchKeywordsFromRedis(int count) {
        if (redisTemplate == null) {
            log.warn("Redis not available, falling back to MySQL method");
            return convertToDto(getTopSearchKeywords());
        }

        log.debug("Retrieving top {} search keywords from Redis", count);

        try {
            // Redis SortedSet ZREVRANGE - O(log N + M) 시간 복잡도
            Set<ZSetOperations.TypedTuple<Object>> results =
                    redisTemplate.opsForZSet().reverseRangeWithScores(POPULAR_KEYWORDS_KEY, 0, count - 1);

            List<PopularKeywordDto> keywords = new ArrayList<>();
            if (results != null) {
                for (ZSetOperations.TypedTuple<Object> tuple : results) {
                    String keyword = tuple.getValue() != null ? tuple.getValue().toString() : "";
                    keywords.add(new PopularKeywordDto(
                            keyword,
                            tuple.getScore() != null ? tuple.getScore().longValue() : 0L
                    ));
                }
            }

            log.info("Retrieved {} top search keywords from Redis", keywords.size());
            return keywords;

        } catch (Exception e) {
            log.error("Failed to retrieve keywords from Redis, falling back to MySQL", e);
            return convertToDto(getTopSearchKeywords());
        }
    }

    /**
     * Redis 기반 인기 검색어 조회 (기본 10개)
     */
    public List<PopularKeywordDto> getTopSearchKeywordsFromRedis() {
        return getTopSearchKeywordsFromRedis(10);
    }

    /**
     * 비동기로 검색 키워드 기록 - 완전히 독립적인 트랜잭션으로 실행
     * 검색 API의 메인 트랜잭션과 완전 분리하여 롤백 이슈 방지
     *
     * @param keyword 검색된 키워드
     * @return CompletableFuture<Void>
     */
    @Async
    @Transactional(propagation = REQUIRES_NEW)
    public CompletableFuture<Void> recordSearchKeywordAsync(String keyword) {
        try {
            recordSearchKeywordWithRedis(keyword);
            log.debug("Successfully recorded keyword '{}' asynchronously", keyword);
        } catch (Exception e) {
            log.warn("Failed to record search keyword '{}' asynchronously: {}", keyword, e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }


    // ======== 헬퍼 메서드들 ========

    private List<PopularKeywordDto> convertToDto(List<SearchKeyword> keywords) {
        return keywords.stream()
                .map(k -> new PopularKeywordDto(k.getKeyword(), k.getSearchCount()))
                .toList();
    }

}