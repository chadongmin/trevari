package com.trevari.book.dto.response;

import com.trevari.global.dto.PageInfo;
import java.util.List;

public record BookSearchResponse(
        String searchQuery,
        PageInfo pageInfo,
        List<BookResponse> books,
        SearchMetadata searchMetadata
) {}