package com.trevari.global.dto;

import org.springframework.data.domain.Page;

public record PageInfo(
        int currentPage,
        int pageSize,
        int totalPages,
        long totalElements
) {
    public static PageInfo from(Page<?> page) {
        return new PageInfo(
                page.getNumber() + 1, // 0-based를 1-based로 변환
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements()
        );
    }
}