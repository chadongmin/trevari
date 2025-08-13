package com.trevari.book.dto;

import lombok.Getter;

/**
 * 인기 키워드 DTO
 */
@Getter
public class PopularKeywordDto {
    
    private final String keyword;
    private final Long count;
    
    public PopularKeywordDto(String keyword, Long count) {
        this.keyword = keyword;
        this.count = count;
    }
    
    @Override
    public String toString() {
        return String.format("PopularKeyword{keyword='%s', count=%d}", keyword, count);
    }
}