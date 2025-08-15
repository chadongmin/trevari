package com.trevari.book.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Table(name = "book")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "도서 정보")
public class Book {
    @Id
    @Schema(description = "도서 ISBN", example = "9781617297397")
    private String isbn;

    @Schema(description = "도서 제목", example = "Java in Action")
    private String title;
    
    @Schema(description = "도서 부제목", example = "Lambdas, streams, functional and reactive programming")
    private String subtitle;

    @Schema(description = "도서 이미지 URL", example = "https://books.google.com/books/content?id=0pkyCwAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api")
    private String imageUrl;

    @Embedded
    @Schema(description = "출판 정보")
    private PublicationInfo publicationInfo;
    
    @Schema(description = "도서 설명")
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Schema(description = "페이지 수", example = "300")
    private Integer pageCount;
    
    @Schema(description = "도서 형태")
    @Enumerated(EnumType.STRING)
    private BookFormat format;
    
    @Embedded
    @Schema(description = "가격 정보")
    private Price price;
    
    @ManyToMany
    @JoinTable(
        name = "book_category",
        joinColumns = @JoinColumn(name = "book_isbn"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Schema(description = "카테고리 목록")
    private Set<Category> categories;
    
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true, fetch = jakarta.persistence.FetchType.EAGER)
    @Schema(description = "도서-저자 관계 목록")
    private Set<BookAuthor> bookAuthors;
    
}

