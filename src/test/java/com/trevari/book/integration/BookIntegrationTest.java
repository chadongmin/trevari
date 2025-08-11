package com.trevari.book.integration;

import com.trevari.book.IntegrationTestSupport;
import com.trevari.book.domain.Book;
import com.trevari.book.persistence.BookJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
@DisplayName("Book API 통합 테스트 - TestContainers")
class BookIntegrationTest extends IntegrationTestSupport {

    @LocalServerPort
    private int port;

    @Autowired
    private BookJpaRepository bookRepository;

    private Book testBook;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 설정
        bookRepository.deleteAll();

        testBook = Book.builder()
                .isbn("9781617297397")
                .title("Java in Action")
                .subtitle("Lambdas, streams, functional and reactive programming")
                .authors(List.of("Raoul-Gabriel Urma", "Mario Fusco", "Alan Mycroft"))
                .publisher("Manning Publications")
                .publishedDate(LocalDate.of(2020, 1, 1))
                .build();

        bookRepository.save(testBook);
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
                .andExpect(jsonPath("$.data.authors").isArray())
                .andExpect(jsonPath("$.data.authors[0]").value("Raoul-Gabriel Urma"))
                .andExpect(jsonPath("$.data.authors[1]").value("Mario Fusco"))
                .andExpect(jsonPath("$.data.authors[2]").value("Alan Mycroft"))
                .andExpect(jsonPath("$.data.publisher").value(testBook.getPublisher()))
                .andExpect(jsonPath("$.data.publishedDate").value(testBook.getPublishedDate().toString()))
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