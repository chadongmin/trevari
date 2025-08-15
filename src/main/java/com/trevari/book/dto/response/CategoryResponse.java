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
    String name,
    
    @Schema(description = "카테고리에 속한 책 수", example = "150")
    Long bookCount
) {
    
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            null  // bookCount는 별도로 설정
        );
    }
    
    public static CategoryResponse of(Long id, String name, Long bookCount) {
        return new CategoryResponse(id, name, bookCount);
    }
}