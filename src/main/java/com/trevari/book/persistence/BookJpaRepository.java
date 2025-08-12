package com.trevari.book.persistence;

import com.trevari.book.domain.Book;
import com.trevari.book.domain.BookRepository;
import com.trevari.book.domain.search.SearchQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookJpaRepository extends JpaRepository<Book, String>, BookRepository {

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
    
    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN b.publicationInfo.authors a " +
           "WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(COALESCE(b.subtitle, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(COALESCE(a, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Book> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN b.publicationInfo.authors a " +
           "WHERE (LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword1, '%')) " +
           "OR LOWER(COALESCE(b.subtitle, '')) LIKE LOWER(CONCAT('%', :keyword1, '%')) " +
           "OR LOWER(COALESCE(a, '')) LIKE LOWER(CONCAT('%', :keyword1, '%'))) " +
           "OR (LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword2, '%')) " +
           "OR LOWER(COALESCE(b.subtitle, '')) LIKE LOWER(CONCAT('%', :keyword2, '%')) " +
           "OR LOWER(COALESCE(a, '')) LIKE LOWER(CONCAT('%', :keyword2, '%')))")
    Page<Book> findByOrKeywords(@Param("keyword1") String keyword1, 
                                @Param("keyword2") String keyword2, 
                                Pageable pageable);
    
    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN b.publicationInfo.authors a " +
           "WHERE (LOWER(b.title) LIKE LOWER(CONCAT('%', :includeKeyword, '%')) " +
           "OR LOWER(COALESCE(b.subtitle, '')) LIKE LOWER(CONCAT('%', :includeKeyword, '%')) " +
           "OR LOWER(COALESCE(a, '')) LIKE LOWER(CONCAT('%', :includeKeyword, '%'))) " +
           "AND NOT (LOWER(b.title) LIKE LOWER(CONCAT('%', :excludeKeyword, '%')) " +
           "OR LOWER(COALESCE(b.subtitle, '')) LIKE LOWER(CONCAT('%', :excludeKeyword, '%')) " +
           "OR LOWER(COALESCE(a, '')) LIKE LOWER(CONCAT('%', :excludeKeyword, '%')))")
    Page<Book> findByNotKeywords(@Param("includeKeyword") String includeKeyword,
                                 @Param("excludeKeyword") String excludeKeyword,
                                 Pageable pageable);
}