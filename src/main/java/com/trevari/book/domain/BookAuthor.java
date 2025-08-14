package com.trevari.book.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Book과 Author 간의 연결 엔티티
 * 저자의 역할 정보를 포함한 관계를 관리
 */
@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookAuthor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "book_isbn")
    private Book book;
    
    @ManyToOne
    @JoinColumn(name = "author_id")
    private Author author;
    
    private String role;
}