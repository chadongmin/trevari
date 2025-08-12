package com.trevari.book.dto.response;

public record SearchMetadata(
        long executionTimeMs,
        String strategy
) {
    public static SearchMetadata of(long executionTime, String strategy) {
        return new SearchMetadata(executionTime, strategy);
    }

    public enum SearchStrategy {
        SIMPLE,
        OR_OPERATION,
        NOT_OPERATION
    }
}