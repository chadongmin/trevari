package com.trevari.book.domain.search;

import java.util.List;

/**
 * 검색 쿼리를 나타내는 도메인 객체
 */
public record SearchQuery(
        String originalQuery,
        SearchStrategy strategy,
        List<String> keywords
) {

    public enum SearchStrategy {
        SIMPLE,         // 단순 키워드 검색
        OR_OPERATION,   // OR 연산 (keyword1 | keyword2)
        NOT_OPERATION   // NOT 연산 (keyword1 -keyword2)
    }

    /**
     * 단순 검색 쿼리 생성
     */
    public static SearchQuery simple(String keyword) {
        return new SearchQuery(keyword, SearchStrategy.SIMPLE, List.of(keyword.trim()));
    }

    /**
     * OR 연산 검색 쿼리 생성
     */
    public static SearchQuery or(String originalQuery, String keyword1, String keyword2) {
        return new SearchQuery(originalQuery, SearchStrategy.OR_OPERATION,
                List.of(keyword1.trim(), keyword2.trim()));
    }

    /**
     * NOT 연산 검색 쿼리 생성
     */
    public static SearchQuery not(String originalQuery, String includeKeyword, String excludeKeyword) {
        return new SearchQuery(originalQuery, SearchStrategy.NOT_OPERATION,
                List.of(includeKeyword.trim(), excludeKeyword.trim()));
    }

    /**
     * 첫 번째 키워드 반환
     */
    public String getFirstKeyword() {
        return keywords.isEmpty() ? "" : keywords.get(0);
    }

    /**
     * 두 번째 키워드 반환 (OR/NOT 연산시)
     */
    public String getSecondKeyword() {
        return keywords.size() < 2 ? "" : keywords.get(1);
    }

    /**
     * NOT 연산에서 포함할 키워드
     */
    public String getIncludeKeyword() {
        if (strategy != SearchStrategy.NOT_OPERATION) {
            throw new IllegalStateException("This is not a NOT operation query");
        }
        return getFirstKeyword();
    }

    /**
     * NOT 연산에서 제외할 키워드
     */
    public String getExcludeKeyword() {
        if (strategy != SearchStrategy.NOT_OPERATION) {
            throw new IllegalStateException("This is not a NOT operation query");
        }
        return getSecondKeyword();
    }
}