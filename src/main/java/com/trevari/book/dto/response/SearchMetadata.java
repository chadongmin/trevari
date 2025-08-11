package com.trevari.book.dto.response;

public record SearchMetadata(
        long executionTime,
        SearchStrategy strategy
) {
    public enum SearchStrategy {
        SIMPLE,
        OR_OPERATION,
        NOT_OPERATION
    }
}