package com.trevari.book.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trevari.book.domain.Book;
import com.trevari.global.dto.PageInfo;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Redis 캐싱용 검색 결과 DTO (execution time 제외)
 * execution time은 매 API 호출마다 새로 계산되어야 하므로 캐시에서 제외
 */
@Getter
@Builder
public class CacheableBookSearchResult {
    private final String searchQuery;
    private final PageInfo pageInfo;
    private final List<BookResponse> books;
    private final String strategy;
    
    @JsonCreator
    public CacheableBookSearchResult(
            @JsonProperty("searchQuery") String searchQuery,
            @JsonProperty("pageInfo") PageInfo pageInfo,
            @JsonProperty("books") List<BookResponse> books,
            @JsonProperty("strategy") String strategy) {
        this.searchQuery = searchQuery;
        this.pageInfo = pageInfo;
        this.books = books;
        this.strategy = strategy;
    }
    
    public static CacheableBookSearchResult from(String searchQuery, PageInfo pageInfo, List<Book> books, String strategy) {
        List<BookResponse> bookResponses = books.stream()
                .map(BookResponse::from)
                .toList();
                
        return CacheableBookSearchResult.builder()
                .searchQuery(searchQuery)
                .pageInfo(pageInfo)
                .books(bookResponses)
                .strategy(strategy)
                .build();
    }
    
    /**
     * 캐시된 결과를 실제 응답 DTO로 변환 (execution time 추가)
     */
    public BookSearchResponse toResponse(long executionTimeMs) {
        return BookSearchResponse.builder()
                .searchQuery(searchQuery)
                .pageInfo(pageInfo)
                .books(books)
                .searchMetadata(SearchMetadata.of(executionTimeMs, strategy))
                .build();
    }
}