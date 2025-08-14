package com.trevari.book.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.trevari.book.domain.Book;
import com.trevari.book.domain.BookFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Collections;

@Schema(description = "도서 정보 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookResponse(
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
    
    @Schema(description = "저자 목록")
    List<String> authors,
    
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
    
    public static BookResponse from(Book book) {
        try {
            return new BookResponse(
                book.getIsbn(),
                book.getTitle(),
                book.getSubtitle(),
                book.getDescription(),
                book.getPageCount(),
                book.getFormat(),
                book.getPrice() != null ? PriceResponse.from(book.getPrice()) : null,
                // BookAuthor에서 저자 이름 추출
                book.getBookAuthors() != null ? 
                    book.getBookAuthors().stream()
                        .map(ba -> ba.getAuthor().getName())
                        .toList() : 
                    Collections.emptyList(),
                // 카테고리 정보
                book.getCategories() != null ?
                    book.getCategories().stream()
                        .map(CategoryResponse::from)
                        .toList() :
                    Collections.emptyList(),
                // PublicationInfo에서 publisher와 publishedDate를 직접 가져오기
                book.getPublicationInfo() != null ? book.getPublicationInfo().getPublisher() : "",
                book.getPublicationInfo() != null ? book.getPublicationInfo().getPublishedDate() : null,
                book.getImageUrl()
            );
        } catch (Exception e) {
            // 오류 발생 시 기본값으로 BookResponse 생성
            return new BookResponse(
                book.getIsbn(),
                book.getTitle(),
                book.getSubtitle(),
                null, null, null, null,
                Collections.emptyList(),
                Collections.emptyList(),
                "",
                null,
                book.getImageUrl()
            );
        }
    }
}