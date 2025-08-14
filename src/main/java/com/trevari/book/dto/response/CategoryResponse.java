package com.trevari.book.dto.response;

import com.trevari.book.domain.Category;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 카테고리 응답 DTO
 */
@Schema(description = "카테고리 정보")
public record CategoryResponse(
    @Schema(description = "카테고리 ID", example = "1")
    Long id,
    
    @Schema(description = "카테고리 이름", example = "프로그래밍")
    String name
) {
    
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getName()
        );
    }
}