package com.trevari.book.integration;

import com.trevari.book.domain.Author;
import com.trevari.book.domain.Book;
import com.trevari.book.domain.BookAuthor;
import com.trevari.book.domain.PublicationInfo;
import com.trevari.book.persistence.AuthorJpaRepository;
import com.trevari.book.persistence.BookAuthorJpaRepository;
import com.trevari.book.persistence.BookJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
@ActiveProfiles("test")
@DisplayName("Book API 통합 테스트")
class BookIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookJpaRepository bookRepository;

    @Autowired
    private AuthorJpaRepository authorRepository;

    @Autowired
    private BookAuthorJpaRepository bookAuthorRepository;

    private Book testBook;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 설정
        bookAuthorRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();

        // 저자 생성
        Author author1 = Author.builder()
                .name("Raoul-Gabriel Urma")
                .build();
        Author author2 = Author.builder()
                .name("Mario Fusco")
                .build();
        Author author3 = Author.builder()
                .name("Alan Mycroft")
                .build();

        author1 = authorRepository.save(author1);
        author2 = authorRepository.save(author2);
        author3 = authorRepository.save(author3);

        // 도서 생성
        testBook = Book.builder()
                .isbn("9781617297397")
                .title("Java in Action")
                .subtitle("Lambdas, streams, functional and reactive programming")
                .publicationInfo(PublicationInfo.builder()
                        .publisher("Manning Publications")
                        .publishedDate(LocalDate.of(2020, 1, 1))
                        .build())
                .build();

        testBook = bookRepository.save(testBook);

        // 도서-저자 관계 생성
        Set<BookAuthor> bookAuthors = new HashSet<>();
        
        BookAuthor bookAuthor1 = BookAuthor.builder()
                .book(testBook)
                .author(author1)
                .role("저자")
                .build();
        BookAuthor bookAuthor2 = BookAuthor.builder()
                .book(testBook)
                .author(author2)
                .role("저자")
                .build();
        BookAuthor bookAuthor3 = BookAuthor.builder()
                .book(testBook)
                .author(author3)
                .role("저자")
                .build();

        bookAuthor1 = bookAuthorRepository.save(bookAuthor1);
        bookAuthor2 = bookAuthorRepository.save(bookAuthor2);
        bookAuthor3 = bookAuthorRepository.save(bookAuthor3);
        
        bookAuthors.add(bookAuthor1);
        bookAuthors.add(bookAuthor2);
        bookAuthors.add(bookAuthor3);

        // Update the testBook's bookAuthors collection
        testBook = Book.builder()
                .isbn(testBook.getIsbn())
                .title(testBook.getTitle())
                .subtitle(testBook.getSubtitle())
                .publicationInfo(testBook.getPublicationInfo())
                .bookAuthors(bookAuthors)
                .build();
        
        testBook = bookRepository.save(testBook);
        bookRepository.flush();
    }

    @Test
    @DisplayName("도서 상세 조회 API 통합 테스트")
    void getBookDetail_IntegrationTest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/books/{isbn}", testBook.getIsbn())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Book retrieved successfully"))
                .andExpect(jsonPath("$.data.isbn").value(testBook.getIsbn()))
                .andExpect(jsonPath("$.data.title").value(testBook.getTitle()))
                .andExpect(jsonPath("$.data.subtitle").value(testBook.getSubtitle()))
                .andExpect(jsonPath("$.data.bookAuthors").isArray())
                .andExpect(jsonPath("$.data.bookAuthors[0].authorName").value("Raoul-Gabriel Urma"))
                .andExpect(jsonPath("$.data.bookAuthors[1].authorName").value("Mario Fusco"))
                .andExpect(jsonPath("$.data.bookAuthors[2].authorName").value("Alan Mycroft"))
                .andExpect(jsonPath("$.data.publisher").value("Manning Publications"))
                .andExpect(jsonPath("$.data.publishedDate").value("2020-01-01"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("존재하지 않는 도서 조회 API 통합 테스트")
    void getBookDetail_NotFound_IntegrationTest() throws Exception {
        // Given
        String nonExistentIsbn = "9999999999999";

        // When & Then
        mockMvc.perform(get("/api/books/{isbn}", nonExistentIsbn)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].code").value("BOOK_NOT_FOUND"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}