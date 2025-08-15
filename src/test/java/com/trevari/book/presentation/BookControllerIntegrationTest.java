package com.trevari.book.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.trevari.book.domain.Author;
import com.trevari.book.domain.Book;
import com.trevari.book.domain.BookAuthor;
import com.trevari.book.domain.PublicationInfo;
import com.trevari.book.persistence.BookJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("BookController 통합 테스트")
class BookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookJpaRepository bookRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    private Book testBook1;
    private Book testBook2;
    private Book testBook3;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 저자 생성
        Author raoul = Author.builder().name("Raoul-Gabriel Urma").build();
        Author mario = Author.builder().name("Mario Fusco").build();
        Author alan = Author.builder().name("Alan Mycroft").build();
        Author joshua = Author.builder().name("Joshua Bloch").build();
        Author martin = Author.builder().name("Martin Kleppmann").build();

        entityManager.persist(raoul);
        entityManager.persist(mario);
        entityManager.persist(alan);
        entityManager.persist(joshua);
        entityManager.persist(martin);
        entityManager.flush();

        // 테스트 데이터 생성
        testBook1 = Book.builder()
                .isbn("9781617297397")
                .title("Modern Java in Action")
                .subtitle("Lambdas, streams, functional and reactive programming")
                .publicationInfo(PublicationInfo.builder()
                        .publisher("Manning Publications")
                        .publishedDate(LocalDate.of(2018, 8, 1))
                        .build())
                .build();

        testBook2 = Book.builder()
                .isbn("9780134685991")
                .title("Effective Java")
                .subtitle("Third Edition")
                .publicationInfo(PublicationInfo.builder()
                        .publisher("Addison-Wesley Professional")
                        .publishedDate(LocalDate.of(2017, 12, 1))
                        .build())
                .build();

        testBook3 = Book.builder()
                .isbn("9781449373320")
                .title("Designing Data-Intensive Applications")
                .publicationInfo(PublicationInfo.builder()
                        .publisher("O'Reilly Media")
                        .publishedDate(LocalDate.of(2017, 3, 1))
                        .build())
                .build();

        bookRepository.saveAll(List.of(testBook1, testBook2, testBook3));

        // BookAuthor 관계 생성
        BookAuthor bookAuthor1_1 = BookAuthor.builder().book(testBook1).author(raoul).role("저자").build();
        BookAuthor bookAuthor1_2 = BookAuthor.builder().book(testBook1).author(mario).role("저자").build();
        BookAuthor bookAuthor1_3 = BookAuthor.builder().book(testBook1).author(alan).role("저자").build();
        BookAuthor bookAuthor2 = BookAuthor.builder().book(testBook2).author(joshua).role("저자").build();
        BookAuthor bookAuthor3 = BookAuthor.builder().book(testBook3).author(martin).role("저자").build();

        entityManager.persist(bookAuthor1_1);
        entityManager.persist(bookAuthor1_2);
        entityManager.persist(bookAuthor1_3);
        entityManager.persist(bookAuthor2);
        entityManager.persist(bookAuthor3);
        entityManager.flush();
    }

    @Test
    @DisplayName("도서 검색 - 단순 키워드 검색 성공")
    void searchBooks_SimpleKeyword_Success() throws Exception {
        mockMvc.perform(get("/api/books")
                        .param("keyword", "Java")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Books search completed successfully"))
                .andExpect(jsonPath("$.data.searchQuery").value("Java"))
                .andExpect(jsonPath("$.data.pageInfo.currentPage").value(1))
                .andExpect(jsonPath("$.data.pageInfo.pageSize").value(10))
                .andExpect(jsonPath("$.data.books").isArray())
                .andExpect(jsonPath("$.data.books.length()").value(2))
                .andExpect(jsonPath("$.data.searchMetadata.strategy").value("SIMPLE"))
                .andExpect(jsonPath("$.data.searchMetadata.executionTimeMs").exists());
    }

    @Test
    @DisplayName("도서 검색 - OR 연산 검색 성공")
    void searchBooks_OrOperation_Success() throws Exception {
        mockMvc.perform(get("/api/books")
                        .param("keyword", "Java | Data")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.searchQuery").value("Java | Data"))
                .andExpect(jsonPath("$.data.books.length()").value(3))
                .andExpect(jsonPath("$.data.searchMetadata.strategy").value("OR_OPERATION"));
    }

    @Test
    @DisplayName("도서 검색 - NOT 연산 검색 성공")
    void searchBooks_NotOperation_Success() throws Exception {
        mockMvc.perform(get("/api/books")
                        .param("keyword", "Java -Modern")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.searchQuery").value("Java -Modern"))
                .andExpect(jsonPath("$.data.books.length()").value(1)) // Only "Effective Java"
                .andExpect(jsonPath("$.data.searchMetadata.strategy").value("NOT_OPERATION"));
    }

    @Test
    @DisplayName("도서 검색 - 저자명으로 검색 성공")
    void searchBooks_ByAuthor_Success() throws Exception {
        mockMvc.perform(get("/api/books")
                        .param("keyword", "Joshua Bloch")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.books.length()").value(1))
                .andExpect(jsonPath("$.data.books[0].isbn").value("9780134685991"));
    }

    @Test
    @DisplayName("도서 검색 - 결과 없음")
    void searchBooks_NoResults() throws Exception {
        mockMvc.perform(get("/api/books")
                        .param("keyword", "NonExistentBook")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.books").isArray())
                .andExpect(jsonPath("$.data.books.length()").value(0))
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(0));
    }

    @Test
    @DisplayName("도서 검색 - 페이징 테스트")
    void searchBooks_Pagination_Success() throws Exception {
        mockMvc.perform(get("/api/books")
                        .param("keyword", "a") // 모든 책에 포함될 만한 키워드
                        .param("page", "1")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageInfo.currentPage").value(1))
                .andExpect(jsonPath("$.data.pageInfo.pageSize").value(2))
                .andExpect(jsonPath("$.data.pageInfo.totalElements").exists())
                .andExpect(jsonPath("$.data.pageInfo.totalPages").exists());
    }

    @Test
    @DisplayName("도서 검색 - 잘못된 키워드")
    void searchBooks_InvalidKeyword() throws Exception {
        mockMvc.perform(get("/api/books")
                        .param("keyword", "")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("도서 검색 - 기본 페이징 파라미터")
    void searchBooks_DefaultPagination() throws Exception {
        mockMvc.perform(get("/api/books")
                        .param("keyword", "Java")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageInfo.currentPage").value(1))
                .andExpect(jsonPath("$.data.pageInfo.pageSize").value(20));
    }
}