package com.trevari.book.domain.search;

/**
 * 검색 메타데이터를 담는 객체
 * API 응답에 포함되어 검색 실행 시간과 전략 정보를 제공
 */
public record SearchMetadata(
        long executionTimeMs,
        SearchQuery.SearchStrategy strategy
) {

    public static SearchMetadata of(long executionTimeMs, SearchQuery.SearchStrategy strategy) {
        return new SearchMetadata(executionTimeMs, strategy);
    }
}