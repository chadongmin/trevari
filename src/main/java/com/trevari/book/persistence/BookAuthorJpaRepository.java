package com.trevari.book.persistence;

import com.trevari.book.domain.BookAuthor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookAuthorJpaRepository extends JpaRepository<BookAuthor, Long> {
}
