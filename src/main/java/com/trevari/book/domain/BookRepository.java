package com.trevari.book.domain;

import java.util.Optional;

public interface BookRepository {

    Optional<Book> findByIsbn(String isbn);
}