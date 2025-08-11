package com.trevari.book.domain.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 검색 쿼리 문자열을 파싱하여 SearchQuery 객체로 변환하는 클래스
 * <p>
 * 지원하는 연산자:
 * - | (OR 연산): keyword1 | keyword2
 * - - (NOT 연산): keyword1 -keyword2
 * <p>
 * 제약사항:
 * - 최대 2개의 키워드만 지원
 * - 연산자가 없으면 단순 검색
 */
@Slf4j
@Component
public class SearchQueryParser {

    private static final Pattern OR_PATTERN = Pattern.compile("^(.+?)\\s*\\|\\s*(.+)$");
    private static final Pattern NOT_PATTERN = Pattern.compile("^(.+?)\\s+-(.+)$");
    private static final int MAX_KEYWORDS = 2;

    /**
     * 검색 쿼리 문자열을 파싱하여 SearchQuery 객체 반환
     *
     * @param queryString 검색 쿼리 문자열
     * @return 파싱된 SearchQuery 객체
     * @throws IllegalArgumentException 잘못된 쿼리 형식인 경우
     */
    public SearchQuery parse(String queryString) {
        if (!StringUtils.hasText(queryString)) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        String trimmedQuery = queryString.trim();
        log.debug("Parsing search query: {}", trimmedQuery);

        // OR 연산자 검사
        Matcher orMatcher = OR_PATTERN.matcher(trimmedQuery);
        if (orMatcher.matches()) {
            String keyword1 = orMatcher.group(1);
            String keyword2 = orMatcher.group(2);

            validateKeywords(keyword1, keyword2);
            log.debug("Parsed as OR operation: {} | {}", keyword1, keyword2);
            return SearchQuery.or(trimmedQuery, keyword1, keyword2);
        }

        // NOT 연산자 검사
        Matcher notMatcher = NOT_PATTERN.matcher(trimmedQuery);
        if (notMatcher.matches()) {
            String includeKeyword = notMatcher.group(1);
            String excludeKeyword = notMatcher.group(2);

            validateKeywords(includeKeyword, excludeKeyword);
            log.debug("Parsed as NOT operation: {} -{}", includeKeyword, excludeKeyword);
            return SearchQuery.not(trimmedQuery, includeKeyword, excludeKeyword);
        }

        // 단순 검색
        validateSimpleKeyword(trimmedQuery);
        log.debug("Parsed as simple search: {}", trimmedQuery);
        return SearchQuery.simple(trimmedQuery);
    }

    private void validateKeywords(String keyword1, String keyword2) {
        if (!StringUtils.hasText(keyword1) || !StringUtils.hasText(keyword2)) {
            throw new IllegalArgumentException("Both keywords must be non-empty");
        }

        if (keyword1.trim().equals(keyword2.trim())) {
            throw new IllegalArgumentException("Keywords cannot be identical");
        }

        validateKeywordLength(keyword1.trim());
        validateKeywordLength(keyword2.trim());
    }

    private void validateSimpleKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new IllegalArgumentException("Keyword cannot be empty");
        }

        validateKeywordLength(keyword.trim());

        // 단순 검색에서도 최대 키워드 수 확인 (공백으로 분리)
        String[] words = keyword.trim().split("\\s+");
        if (words.length > MAX_KEYWORDS) {
            throw new IllegalArgumentException("Maximum " + MAX_KEYWORDS + " keywords allowed");
        }
    }

    private void validateKeywordLength(String keyword) {
        if (keyword.length() > 100) { // 키워드 최대 길이 제한
            throw new IllegalArgumentException("Keyword too long (max 100 characters)");
        }

        // 특수 문자 검사 (알파벳, 숫자, 한글, 공백만 허용)
        if (!keyword.matches("^[a-zA-Z0-9가-힣\\s]+$")) {
            throw new IllegalArgumentException("Invalid characters in keyword");
        }
    }
}