package com.trevari.book.dto.response;

import com.trevari.book.domain.Book;

import java.time.LocalDate;
import java.util.List;

public record BookResponse(
        String isbn,
        String title,
        String subtitle,
        List<String> authors,
        String publisher,
        LocalDate publishedDate
) {
    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getIsbn(),
                book.getTitle(),
                book.getSubtitle(),
                book.getAuthors(),
                book.getPublisher(),
                book.getPublishedDate()
        );
    }
}