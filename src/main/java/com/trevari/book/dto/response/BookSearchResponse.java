package com.trevari.book.dto.response;

import com.trevari.book.domain.Book;
import com.trevari.global.dto.PageInfo;
import lombok.Builder;

import java.util.List;

@Builder
public record BookSearchResponse(
        String searchQuery,
        PageInfo pageInfo,
        List<Book> books,
        SearchMetadata searchMetadata
) {}