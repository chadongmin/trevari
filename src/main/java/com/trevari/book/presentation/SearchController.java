package com.trevari.book.presentation;

import com.trevari.book.application.BookService;
import com.trevari.book.application.SearchKeywordService;
import com.trevari.book.domain.SearchKeyword;
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
@Tag(name = "Search API", description = "도서 검색 및 인기 키워드 API")
public class SearchController {
    
    private final BookService bookService;
    private final SearchKeywordService searchKeywordService;
    
    /**
     * 도서 검색 (명시적 검색 API)
     *
     * @param q    검색 쿼리
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @return 검색 결과
     */
    @Operation(summary = "도서 검색", description = "검색 쿼리로 도서를 검색합니다. OR(|), NOT(-) 연산자를 지원합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "검색 성공",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 검색 쿼리",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @GetMapping("/books")
    @RateLimit(limit = 100, window = 1) // 100 requests per minute per IP
    public ResponseEntity<ApiResponse<BookSearchResponse>> searchBooks(
        @Parameter(description = "검색 쿼리 (OR: keyword1|keyword2, NOT: keyword1 -keyword2)", required = true, example = "Java")
        @RequestParam String keyword,
        @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
        @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "페이지 크기", example = "20")
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
    
    /**
     * 전체 도서 목록 조회 (페이징)
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @return 전체 도서 목록
     */
    @Operation(summary = "전체 도서 목록 조회", description = "전체 도서를 페이징하여 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @GetMapping("/all")
    @RateLimit(limit = 100, window = 1) // 100 requests per minute per IP
    public ResponseEntity<ApiResponse<BookSearchResponse>> getAllBooks(
        @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
        @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20") int size) {
        
        log.info("Request to get all books - page: {}, size: {}", page, size);
        
        // 페이지 번호를 0 기반으로 변환 (Spring Data는 0부터 시작)
        Pageable pageable = PageRequest.of(page - 1, size);
        
        BookSearchResponse response = bookService.getAllBooks(pageable);
        
        return ApiResponse.ok(response, "All books retrieved successfully");
    }

    /**
     * 인기 검색 키워드 조회 (상위 10개)
     *
     * @return 인기 검색 키워드 목록 (검색 횟수와 함께)
     */
    @Operation(summary = "인기 검색 키워드", description = "검색 횟수 기준 상위 10개 인기 키워드를 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @GetMapping("/popular")
    @RateLimit(limit = 20, window = 1) // 20 requests per minute per IP  
    public ResponseEntity<ApiResponse<PopularSearchResponse>> getPopularKeywords() {
        log.info("Request to get popular search keywords");
        
        List<SearchKeyword> topKeywords = searchKeywordService.getTopSearchKeywords();
        PopularSearchResponse response = PopularSearchResponse.from(topKeywords);
        
        return ApiResponse.ok(response, "Popular search keywords retrieved successfully");
    }
}