package com.trevari.book.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface BookRepository {
    
    Optional<Book> findByIsbn(String isbn);
    
    Page<Book> findAll(Pageable pageable);
    
    Page<Book> findByKeyword(String keyword, Pageable pageable);
    
    Page<Book> findByKeywordOr(String keyword1, String keyword2, Pageable pageable);
    
    Page<Book> findByKeywordNot(String keyword1, String keyword2, Pageable pageable);
    
    Book save(Book book);
    
    void deleteByIsbn(String isbn);
    
    boolean existsByIsbn(String isbn);
}