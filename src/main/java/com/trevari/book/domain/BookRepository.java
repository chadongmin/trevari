package com.trevari.book.domain;

import com.trevari.book.domain.search.SearchQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface BookRepository {

    Optional<Book> findByIsbn(String isbn);

    /**
     * 검색 쿼리에 따라 도서를 검색합니다.
     * 
     * @param searchQuery 검색 쿼리 객체
     * @param pageable 페이징 정보
     * @return 검색된 도서 페이지
     */
    Page<Book> searchBooks(SearchQuery searchQuery, Pageable pageable);
}