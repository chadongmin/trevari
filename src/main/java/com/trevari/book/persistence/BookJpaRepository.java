package com.trevari.book.persistence;

import com.trevari.book.domain.Book;
import com.trevari.book.domain.BookRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookJpaRepository extends JpaRepository<Book, String>, BookRepository {

    Optional<Book> findByIsbn(String isbn);
}