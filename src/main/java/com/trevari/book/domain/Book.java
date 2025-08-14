package com.trevari.book.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
}

