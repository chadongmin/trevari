package com.trevari.book.application;

import com.trevari.book.domain.SearchKeyword;
import com.trevari.book.domain.SearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    private RedisTemplate<String, String> redisTemplate;
    
    private static final String POPULAR_KEYWORDS_KEY = "popular_keywords";

    /**
     * 검색 키워드 사용 기록
     * 
     * @param keyword 검색된 키워드
     */
    @Transactional
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
            
            // 선택적: 비동기로 DB에도 백업 (일관성 보장)
            asyncBackupToDatabase(normalizedKeyword);
            
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
            Set<ZSetOperations.TypedTuple<String>> results = 
                redisTemplate.opsForZSet().reverseRangeWithScores(POPULAR_KEYWORDS_KEY, 0, count - 1);
            
            List<PopularKeywordDto> keywords = new ArrayList<>();
            if (results != null) {
                for (ZSetOperations.TypedTuple<String> tuple : results) {
                    keywords.add(new PopularKeywordDto(
                        tuple.getValue(), 
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
     * Redis와 MySQL 성능 비교를 위한 통합 메서드
     * 
     * @param useRedis true면 Redis, false면 MySQL 사용
     * @return 성능 측정 결과와 함께 키워드 목록
     */
    public KeywordPerformanceResult getTopKeywordsWithPerformanceComparison(boolean useRedis) {
        long startTime = System.currentTimeMillis();
        
        List<PopularKeywordDto> keywords;
        String method;
        
        if (useRedis && redisTemplate != null) {
            keywords = getTopSearchKeywordsFromRedis();
            method = "Redis SortedSet";
        } else {
            keywords = convertToDto(getTopSearchKeywords());
            method = "MySQL";
        }
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        log.info("Method: {}, Execution time: {}ms, Keywords count: {}", 
                method, executionTime, keywords.size());
        
        return new KeywordPerformanceResult(keywords, method, executionTime);
    }
    
    /**
     * 동시성 테스트를 위한 Redis 기반 키워드 기록 메서드
     */
    public void recordKeywordForConcurrencyTest(String keyword) {
        recordSearchKeywordWithRedis(keyword);
    }
    
    // ======== 헬퍼 메서드들 ========
    
    private void asyncBackupToDatabase(String keyword) {
        // 실제 구현에서는 @Async 메서드나 메시지 큐 사용
        // 여기서는 간단히 동기식으로 처리
        try {
            searchKeywordRepository.findByKeyword(keyword)
                .ifPresentOrElse(
                    existingKeyword -> searchKeywordRepository.incrementSearchCount(keyword),
                    () -> {
                        SearchKeyword newKeyword = SearchKeyword.builder()
                                .keyword(keyword)
                                .build();
                        searchKeywordRepository.saveSearchKeyword(newKeyword);
                    }
                );
        } catch (Exception e) {
            log.warn("Failed to backup keyword '{}' to database", keyword, e);
        }
    }
    
    private List<PopularKeywordDto> convertToDto(List<SearchKeyword> keywords) {
        return keywords.stream()
                .map(k -> new PopularKeywordDto(k.getKeyword(), k.getSearchCount()))
                .toList();
    }
    
    // ======== 내부 클래스들 ========
    
    /**
     * 인기 키워드 DTO
     */
    public static class PopularKeywordDto {
        private final String keyword;
        private final Long count;
        
        public PopularKeywordDto(String keyword, Long count) {
            this.keyword = keyword;
            this.count = count;
        }
        
        public String getKeyword() { return keyword; }
        public Long getCount() { return count; }
        
        @Override
        public String toString() {
            return String.format("PopularKeyword{keyword='%s', count=%d}", keyword, count);
        }
    }
    
    /**
     * 성능 비교 결과 클래스
     */
    public static class KeywordPerformanceResult {
        private final List<PopularKeywordDto> keywords;
        private final String method;
        private final long executionTimeMs;
        
        public KeywordPerformanceResult(List<PopularKeywordDto> keywords, String method, long executionTimeMs) {
            this.keywords = keywords;
            this.method = method;
            this.executionTimeMs = executionTimeMs;
        }
        
        public List<PopularKeywordDto> getKeywords() { return keywords; }
        public String getMethod() { return method; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        
        @Override
        public String toString() {
            return String.format("PerformanceResult{method='%s', time=%dms, count=%d}", 
                               method, executionTimeMs, keywords.size());
        }
    }
}