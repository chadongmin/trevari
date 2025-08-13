package com.trevari.book.presentation;

import com.trevari.book.application.BookService;
import com.trevari.book.domain.Book;
import com.trevari.book.dto.response.BookResponse;
import com.trevari.book.dto.response.BookSearchResponse;
import com.trevari.book.exception.BookException;
import com.trevari.book.exception.BookExceptionCode;
import com.trevari.global.dto.ApiResponse;
import com.trevari.global.ratelimit.RateLimit;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 도서 관련 API를 제공하는 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController implements BookApi {

    private final BookService bookService;

    /**
     * ISBN으로 도서 상세 조회
     *
     * @param isbn 도서 ISBN
     * @return 도서 상세 정보
     */
    @Override
    @GetMapping("/{isbn}")
    @RateLimit(limit = 200, window = 1) // 200 requests per minute per IP
    public ResponseEntity<ApiResponse<BookResponse>> getBookDetail(@PathVariable String isbn) {

        if (StringUtils.isBlank(isbn)) {
            throw new BookException(BookExceptionCode.INVALID_SEARCH_KEYWORD);
        }
        log.info("Request to get book detail - ISBN: {}", isbn);

        Book book = bookService.getBookByIsbn(isbn);
        BookResponse bookResponse = BookResponse.from(book);

        return ApiResponse.ok(bookResponse, "Book retrieved successfully");
    }

    /**
     * 키워드로 도서 검색
     *
     * @param keyword 검색 키워드
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @return 검색 결과
     */
    @Override
    @GetMapping
    @RateLimit(limit = 100, window = 1) // 100 requests per minute per IP
    public ResponseEntity<ApiResponse<BookSearchResponse>> searchBooks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (StringUtils.isBlank(keyword)) {
            throw new BookException(BookExceptionCode.INVALID_SEARCH_KEYWORD);
        }

        log.info("Request to search books - keyword: {}, page: {}, size: {}", keyword, page, size);

        // 페이지 번호를 0 기반으로 변환 (Spring Data는 0부터 시작)
        Pageable pageable = PageRequest.of(page - 1, size);
        
        BookSearchResponse response = bookService.searchBooks(keyword, pageable);

        return ApiResponse.ok(response, "Books search completed successfully");
    }
}