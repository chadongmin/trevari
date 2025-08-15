package com.trevari.book.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 인기 카테고리 응답 DTO (책 수 포함)
 */
@Schema(description = "인기 카테고리 정보 (책 수 포함)")
public record PopularCategoryResponse(
    @Schema(description = "카테고리 ID", example = "1")
    Long id,
    
    @Schema(description = "카테고리 이름", example = "프로그래밍")
    String name,
    
    @Schema(description = "카테고리에 속한 책 수", example = "150")
    Long bookCount
) {
    
    public static PopularCategoryResponse of(Long id, String name, Long bookCount) {
        return new PopularCategoryResponse(id, name, bookCount);
    }
}