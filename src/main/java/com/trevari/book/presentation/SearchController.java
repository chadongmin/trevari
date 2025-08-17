package com.trevari.book.presentation;

import com.trevari.book.application.BookService;
import com.trevari.book.application.SearchKeywordService;
import com.trevari.book.dto.PopularKeywordDto;
import com.trevari.book.dto.response.BookSearchResponse;
import com.trevari.book.dto.response.PopularSearchResponse;
import com.trevari.book.exception.BookException;
import com.trevari.book.exception.BookExceptionCode;
import com.trevari.global.dto.ApiResponse;
import com.trevari.global.ratelimit.RateLimit;
import io.micrometer.common.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 검색 관련 API를 제공하는 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController implements SearchApi {
    
    private final BookService bookService;
    private final SearchKeywordService searchKeywordService;
    
    @Override
    @GetMapping("/books")
    @RateLimit(limit = 3, window = 10, timeUnit = java.util.concurrent.TimeUnit.SECONDS)
    public ResponseEntity<ApiResponse<BookSearchResponse>> searchBooks(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size) {
        
        if (StringUtils.isBlank(keyword)) {
            throw new BookException(BookExceptionCode.INVALID_SEARCH_KEYWORD);
        }
        
        log.info("Request to search books via /api/search/books - query: {}, page: {}, size: {}", keyword, page, size);
        
        // 페이지 번호를 0 기반으로 변환 (Spring Data는 0부터 시작)
        Pageable pageable = PageRequest.of(page - 1, size);
        
        BookSearchResponse response = bookService.searchBooks(keyword, pageable);
        
        return ApiResponse.ok(response, "Books search completed successfully");
    }
    
    @Override
    @GetMapping("/popular")
    @RateLimit(limit = 20, window = 1)  
    public ResponseEntity<ApiResponse<PopularSearchResponse>> getPopularKeywords() {
        log.info("SearchKeywordService class: {}", searchKeywordService.getClass().getName());
        
        // Use Redis method for real-time popular keywords
        List<PopularKeywordDto> topKeywords = searchKeywordService.getTopSearchKeywordsFromRedis();
        log.info("Retrieved {} keywords from Redis method", topKeywords.size());
        PopularSearchResponse response = PopularSearchResponse.fromDto(topKeywords);
        
        return ApiResponse.ok(response, "Popular search keywords retrieved successfully");
    }
}