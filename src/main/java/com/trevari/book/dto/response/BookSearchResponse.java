package com.trevari.book.dto.response;

import com.trevari.book.domain.Book;
import com.trevari.global.dto.PageInfo;
import java.util.List;
import lombok.Builder;

@Builder
public record BookSearchResponse(
    String searchQuery,
    PageInfo pageInfo,
    List<BookResponse> books,
    SearchMetadata searchMetadata
) {
    
    public static BookSearchResponse from(String searchQuery, PageInfo pageInfo, List<Book> books, SearchMetadata searchMetadata) {
        List<BookResponse> bookResponses = books.stream()
                .map(BookResponse::from)
                .toList();
                
        return BookSearchResponse.builder()
                .searchQuery(searchQuery)
                .pageInfo(pageInfo)
                .books(bookResponses)
                .searchMetadata(searchMetadata)
                .build();
    }
}
