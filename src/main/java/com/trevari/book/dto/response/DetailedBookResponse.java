package com.trevari.book.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.trevari.book.domain.Book;
import com.trevari.book.domain.BookFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Collections;

/**
 * 도서 상세 정보 응답 DTO (새로운 도메인 모델 정보 포함)
 */
@Schema(description = "도서 상세 정보 응답")
@JsonInclude(JsonInclude.Include.ALWAYS)
public record DetailedBookResponse(
    @Schema(description = "도서 ISBN", example = "9781617297397")
    String isbn,
    
    @Schema(description = "도서 제목", example = "Java in Action")
    String title,
    
    @Schema(description = "도서 부제목", example = "Lambdas, streams, functional and reactive programming")
    String subtitle,
    
    @Schema(description = "도서 설명")
    String description,
    
    @Schema(description = "페이지 수", example = "512")
    Integer pageCount,
    
    @Schema(description = "도서 형태", example = "PAPERBACK")
    BookFormat format,
    
    @Schema(description = "가격 정보")
    PriceResponse price,
    
    @Schema(description = "기존 저자 목록 (하위 호환성)")
    List<String> authors,
    
    @Schema(description = "저자 상세 정보")
    List<BookAuthorResponse> bookAuthors,
    
    @Schema(description = "카테고리 목록")
    List<CategoryResponse> categories,
    
    @Schema(description = "출판사", example = "Manning Publications")
    String publisher,
    
    @Schema(description = "출간일", example = "2020-01-01")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate publishedDate,
    
    @Schema(description = "도서 이미지 URL")
    String imageUrl
) {
    
    public static DetailedBookResponse from(Book book) {
        return new DetailedBookResponse(
            book.getIsbn(),
            book.getTitle(),
            book.getSubtitle(),
            "곧 제공될 상세 설명", // 미래 구현 예정
            450, // 예시 페이지 수
            BookFormat.PAPERBACK, // 기본값
            new PriceResponse(29000, "KRW"), // 예시 가격
            // 기존 PublicationInfo의 authors (하위 호환성)
            book.getPublicationInfo() != null ? book.getPublicationInfo().getAuthors() : List.of(),
            // 새로운 BookAuthor 관계 정보 (미래 구현 예정)
            Collections.emptyList(),
            // 카테고리 정보 (미래 구현 예정)
            List.of(
                new CategoryResponse(1L, "프로그래밍"),
                new CategoryResponse(2L, "Java")
            ),
            book.getPublicationInfo() != null ? book.getPublicationInfo().getPublisher() : "",
            book.getPublicationInfo() != null ? book.getPublicationInfo().getPublishedDate() : null,
            book.getImageUrl()
        );
    }
}