package com.trevari.book.persistence;

import com.trevari.book.domain.Book;
import com.trevari.book.domain.BookRepository;
import com.trevari.book.domain.search.SearchQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface BookJpaRepository extends JpaRepository<Book, String>, BookRepository, CustomBookRepository {

    @Override
    Optional<Book> findByIsbn(String isbn);
    
    @Override
    default Page<Book> searchBooks(SearchQuery searchQuery, Pageable pageable) {
        return switch (searchQuery.strategy()) {
            case SIMPLE -> findByKeyword(searchQuery.getFirstKeyword(), pageable);
            case OR_OPERATION -> findByOrKeywords(searchQuery.getFirstKeyword(), 
                                                 searchQuery.getSecondKeyword(), 
                                                 pageable);
            case NOT_OPERATION -> findByNotKeywords(searchQuery.getIncludeKeyword(), 
                                                   searchQuery.getExcludeKeyword(), 
                                                   pageable);
        };
    }

    @Override
    default Page<Book> findByCategory(String categoryName, Pageable pageable) {
        return findByCategoryName(categoryName, pageable);
    }
}