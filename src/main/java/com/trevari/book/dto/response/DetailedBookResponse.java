package com.trevari.book.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.trevari.book.domain.Book;
import com.trevari.book.domain.BookFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 도서 상세 정보 응답 DTO
 * Book 엔티티의 모든 정보를 포함하여 완전한 상세 정보를 제공
 */
@Schema(description = "도서 상세 정보 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
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
    
    @Schema(description = "저자 상세 정보 (역할 포함)")
    List<BookAuthorResponse> bookAuthors,
    
    @Schema(description = "카테고리 목록")
    List<CategoryResponse> categories,
    
    @Schema(description = "출판사", example = "Manning Publications")
    String publisher,
    
    @Schema(description = "출간일", example = "2020-01-01")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate publishedDate,
    
    @Schema(description = "도서 이미지 URL")
    String imageUrl,
    
    @Schema(description = "하위 호환성을 위한 저자명 목록")
    List<String> authors
) {
    
    /**
     * Book 엔티티로부터 완전한 상세 정보 응답 DTO 생성
     */
    public static DetailedBookResponse from(Book book) {
        if (book == null) {
            return null;
        }
        
        // 가격 정보 처리
        PriceResponse priceResponse = null;
        if (book.getPrice() != null) {
            priceResponse = PriceResponse.from(book.getPrice());
        }
        
        // 저자 정보 처리
        List<BookAuthorResponse> bookAuthorResponses = null;
        List<String> authors = null;
        
        if (book.getBookAuthors() != null && !book.getBookAuthors().isEmpty()) {
            bookAuthorResponses = book.getBookAuthors().stream()
                .sorted((ba1, ba2) -> {
                    if (ba1.getId() == null && ba2.getId() == null) return 0;
                    if (ba1.getId() == null) return 1;
                    if (ba2.getId() == null) return -1;
                    return ba1.getId().compareTo(ba2.getId());
                })
                .map(BookAuthorResponse::from)
                .collect(Collectors.toList());
            
            authors = book.getBookAuthors().stream()
                .sorted((ba1, ba2) -> {
                    if (ba1.getId() == null && ba2.getId() == null) return 0;
                    if (ba1.getId() == null) return 1;
                    if (ba2.getId() == null) return -1;
                    return ba1.getId().compareTo(ba2.getId());
                })
                .map(bookAuthor -> bookAuthor.getAuthor() != null ? bookAuthor.getAuthor().getName() : "Unknown")
                .collect(Collectors.toList());
        }
        
        // 카테고리 정보 처리
        List<CategoryResponse> categoryResponses = null;
        if (book.getCategories() != null && !book.getCategories().isEmpty()) {
            categoryResponses = book.getCategories().stream()
                .map(category -> CategoryResponse.of(category.getId(), category.getName(), null))
                .collect(Collectors.toList());
        }
        
        // 출판 정보 처리
        String publisher = null;
        LocalDate publishedDate = null;
        
        if (book.getPublicationInfo() != null) {
            publisher = book.getPublicationInfo().getNormalizedPublisher();
            publishedDate = book.getPublicationInfo().getPublishedDate();
        }
        
        return new DetailedBookResponse(
            book.getIsbn(),
            book.getTitle(),
            book.getSubtitle(),
            book.getDescription(),
            book.getPageCount(),
            book.getFormat(),
            priceResponse,
            bookAuthorResponses,
            categoryResponses,
            publisher,
            publishedDate,
            book.getImageUrl(),
            authors
        );
    }
}