package com.trevari.book.dto.response;

import com.trevari.book.domain.BookAuthor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 도서-저자 관계 응답 DTO
 */
@Schema(description = "도서-저자 관계 정보")
public record BookAuthorResponse(
    @Schema(description = "저자 ID", example = "1")
    Long authorId,
    
    @Schema(description = "저자 이름", example = "James Gosling")
    String authorName,
    
    @Schema(description = "저자 역할", example = "대표 저자")
    String role
) {
    
    public static BookAuthorResponse from(BookAuthor bookAuthor) {
        return new BookAuthorResponse(
            bookAuthor.getAuthor() != null ? bookAuthor.getAuthor().getId() : null,
            bookAuthor.getAuthor() != null ? bookAuthor.getAuthor().getName() : "",
            bookAuthor.getRole()
        );
    }
}