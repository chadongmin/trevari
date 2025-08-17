package com.trevari.book.presentation;

import com.trevari.book.application.BookService;
import com.trevari.book.dto.response.BookSearchResponse;
import com.trevari.book.dto.response.DetailedBookResponse;
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
     * ISBN으로 도서 상세 조회 (완전한 상세 정보 제공)
     *
     * @param isbn 도서 ISBN
     * @return 도서 상세 정보 (모든 필드 포함)
     */
    @Override
    @GetMapping("/{isbn}")
    @RateLimit(limit = 3, window = 10, timeUnit = java.util.concurrent.TimeUnit.SECONDS) // 10초 동안 3번 제한
    public ResponseEntity<ApiResponse<DetailedBookResponse>> getBookDetail(@PathVariable String isbn) {

        if (StringUtils.isBlank(isbn)) {
            throw new BookException(BookExceptionCode.INVALID_SEARCH_KEYWORD);
        }
        log.info("Request to get book detailed information - ISBN: {}", isbn);

        DetailedBookResponse detailedBook = bookService.getDetailedBookByIsbn(isbn);

        return ApiResponse.ok(detailedBook, "Book retrieved successfully");
    }

    @Override
    @GetMapping("/all")
    @RateLimit(limit = 100, window = 1)
    public ResponseEntity<ApiResponse<BookSearchResponse>> getAllBooks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Request to get all books - page: {}, size: {}", page, size);

        // 페이지 번호를 0 기반으로 변환 (Spring Data는 0부터 시작)
        Pageable pageable = PageRequest.of(page - 1, size);

        BookSearchResponse response = bookService.getAllBooks(pageable);

        return ApiResponse.ok(response, "All books retrieved successfully");
    }

    @Override
    @GetMapping("/category/{categoryName}")
    @RateLimit(limit = 100, window = 1)
    public ResponseEntity<ApiResponse<BookSearchResponse>> getBooksByCategory(
            @PathVariable String categoryName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (StringUtils.isBlank(categoryName)) {
            throw new BookException(BookExceptionCode.INVALID_SEARCH_KEYWORD);
        }

        log.info("Request to get books by category - category: {}, page: {}, size: {}", categoryName, page, size);

        // 페이지 번호를 0 기반으로 변환
        Pageable pageable = PageRequest.of(page - 1, size);
        BookSearchResponse response = bookService.getBooksByCategory(categoryName, pageable);

        return ApiResponse.ok(response, String.format("Books for category '%s' retrieved successfully", categoryName));
    }
}