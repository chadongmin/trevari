package com.trevari.book.application;

import com.trevari.book.domain.SearchKeyword;
import com.trevari.book.domain.SearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 검색 키워드 관리를 담당하는 서비스 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchKeywordService {

    private final SearchKeywordRepository searchKeywordRepository;

    /**
     * 검색 키워드 사용 기록
     * 
     * @param keyword 검색된 키워드
     */
    @Transactional
    public void recordSearchKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }

        String normalizedKeyword = keyword.trim().toLowerCase();
        log.debug("Recording search keyword: {}", normalizedKeyword);

        searchKeywordRepository.findByKeyword(normalizedKeyword)
                .ifPresentOrElse(
                        existingKeyword -> {
                            existingKeyword.incrementCount();
                            log.debug("Incremented count for keyword: {} to {}", 
                                    normalizedKeyword, existingKeyword.getSearchCount());
                        },
                        () -> {
                            SearchKeyword newKeyword = SearchKeyword.builder()
                                    .keyword(normalizedKeyword)
                                    .build();
                            searchKeywordRepository.save(newKeyword);
                            log.debug("Created new search keyword: {}", normalizedKeyword);
                        }
                );
    }

    /**
     * 인기 검색 키워드 조회 (상위 10개)
     * 
     * @return 검색 횟수 기준 상위 10개 키워드 목록
     */
    public List<SearchKeyword> getTopSearchKeywords() {
        log.debug("Retrieving top search keywords");
        
        List<SearchKeyword> topKeywords = searchKeywordRepository.findTop10ByOrderBySearchCountDesc();
        
        log.info("Retrieved {} top search keywords", topKeywords.size());
        return topKeywords;
    }
}