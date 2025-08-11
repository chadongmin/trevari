package com.trevari.book.domain;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "book")
@Getter
public class Book {
    @Id
    private String isbn;

    private String title;
    private String subtitle;

    @ElementCollection
    private List<String> authors;

    private String publisher;
    private LocalDate publishedDate;


}

