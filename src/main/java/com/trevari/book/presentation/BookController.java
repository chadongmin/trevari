package com.trevari.book.presentation;

import com.trevari.book.application.BookService;
import com.trevari.book.domain.Book;
import com.trevari.book.exception.BookException;
import com.trevari.book.exception.BookExceptionCode;
import com.trevari.global.dto.ApiResponse;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<ApiResponse<Book>> getBookDetail(@PathVariable String isbn) {

        if (StringUtils.isBlank(isbn)) {
            throw new BookException(BookExceptionCode.INVALID_SEARCH_KEYWORD);
        }
        log.info("Request to get book detail - ISBN: {}", isbn);

        Book book = bookService.getBookByIsbn(isbn);

        return ApiResponse.ok(book, "Book retrieved successfully");
    }
}