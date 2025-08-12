package com.trevari.book.application;

import com.trevari.book.domain.Book;
import com.trevari.book.domain.BookRepository;
import com.trevari.book.exception.BookException;
import com.trevari.book.exception.BookExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 도서 비즈니스 로직을 담당하는 서비스 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;

    /**
     * ISBN으로 도서 단건 조회
     *
     * @param isbn 도서 ISBN
     * @return 조회된 도서 정보
     * @throws BookException 도서를 찾을 수 없는 경우
     */
    public Book getBookByIsbn(String isbn) {
        log.debug("Finding book by ISBN: {}", isbn);

        return bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> {
                    log.warn("Book not found with ISBN: {}", isbn);
                    return new BookException(BookExceptionCode.BOOK_NOT_FOUND);
                });
    }
}