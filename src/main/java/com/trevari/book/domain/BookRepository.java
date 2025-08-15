package com.trevari.book.domain;

import com.trevari.book.domain.search.SearchQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface BookRepository {

    Optional<Book> findByIsbn(String isbn);

    /**
     * 전체 도서를 페이징하여 조회합니다.
     * 
     * @param pageable 페이징 정보
     * @return 전체 도서 페이지
     */
    Page<Book> findAll(Pageable pageable);

    /**
     * 검색 쿼리에 따라 도서를 검색합니다.
     * 
     * @param searchQuery 검색 쿼리 객체
     * @param pageable 페이징 정보
     * @return 검색된 도서 페이지
     */
    Page<Book> searchBooks(SearchQuery searchQuery, Pageable pageable);

    /**
     * 카테고리별 도서를 조회합니다.
     * 
     * @param categoryName 카테고리명
     * @param pageable 페이징 정보
     * @return 카테고리별 도서 페이지
     */
    Page<Book> findByCategory(String categoryName, Pageable pageable);
}