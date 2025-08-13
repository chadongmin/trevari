package com.trevari.book.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.trevari.book.domain.Book;
import java.time.LocalDate;
import java.util.List;

public record BookResponse(
    String isbn,
    String title,
    String subtitle,
    List<String> authors,
    String publisher,
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate publishedDate
) {
    
    public static BookResponse from(Book book) {
        return new BookResponse(
            book.getIsbn(),
            book.getTitle(),
            book.getSubtitle(),
            book.getPublicationInfo() != null ? book.getPublicationInfo().getAuthors() : List.of(),
            book.getPublicationInfo() != null ? book.getPublicationInfo().getPublisher() : "",
            book.getPublicationInfo() != null ? book.getPublicationInfo().getPublishedDate() : null
        );
    }
}