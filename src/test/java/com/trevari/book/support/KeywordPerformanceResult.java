package com.trevari.book.support;

import com.trevari.book.dto.PopularKeywordDto;

import java.util.List;

/**
 * 성능 비교 결과 클래스 (테스트 전용)
 */
public class KeywordPerformanceResult {
    
    private final List<PopularKeywordDto> keywords;
    private final String method;
    private final long executionTimeMs;
    
    public KeywordPerformanceResult(List<PopularKeywordDto> keywords, String method, long executionTimeMs) {
        this.keywords = keywords;
        this.method = method;
        this.executionTimeMs = executionTimeMs;
    }
    
    public List<PopularKeywordDto> getKeywords() {
        return keywords;
    }
    
    public String getMethod() {
        return method;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    @Override
    public String toString() {
        return String.format("PerformanceResult{method='%s', time=%dms, count=%d}",
            method, executionTimeMs, keywords.size());
    }
}