package com.trevari.book.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record BookSearchRequest(
        String keyword,
        
        @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다")
        int page,
        
        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
        @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
        int size
) {
    public BookSearchRequest {
        if (page <= 0) page = 1;
        if (size <= 0) size = 20;
        if (size > 100) size = 100;
    }
    
    public BookSearchRequest(String keyword) {
        this(keyword, 1, 20);
    }
}