package com.trevari.book.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.trevari.book.application.BookService;
import com.trevari.book.domain.Book;
import com.trevari.book.domain.PublicationInfo;
import com.trevari.book.exception.BookException;
import com.trevari.book.exception.BookExceptionCode;
import com.trevari.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookController 단위 테스트")
class BookControllerTest {

    @Mock
    private BookService bookService;

    @InjectMocks
    private BookController bookController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Book sampleBook;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(bookController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        sampleBook = Book.builder()
                .isbn("9781617297397")
                .title("Java in Action")
                .subtitle("Lambdas, streams, functional and reactive programming")
                .publicationInfo(PublicationInfo.builder()
                        .publisher("Manning Publications")
                        .publishedDate(LocalDate.of(2020, 1, 1))
                        .build())
                .build();
    }

    @Test
    @DisplayName("도서 상세 조회 성공")
    void getBookDetail_Success() throws Exception {
        // given
        String isbn = "9781617297397";
        given(bookService.getBookByIsbn(isbn)).willReturn(sampleBook);

        // when & then
        mockMvc.perform(get("/api/books/{isbn}", isbn)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Book retrieved successfully"))
                .andExpect(jsonPath("$.data.isbn").value("9781617297397"))
                .andExpect(jsonPath("$.data.title").value("Java in Action"))
                .andExpect(jsonPath("$.data.subtitle").value("Lambdas, streams, functional and reactive programming"))
                .andExpect(jsonPath("$.data.authors").isArray())
                .andExpect(jsonPath("$.data.authors").isEmpty()) // Authors moved to BookAuthor entity
                .andExpect(jsonPath("$.data.publisher").value("Manning Publications"))
                .andExpect(jsonPath("$.data.publishedDate").value("2020-01-01"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(bookService).getBookByIsbn(isbn);
    }

    @Test
    @DisplayName("존재하지 않는 도서 조회시 404 반환")
    void getBookDetail_BookNotFound() throws Exception {
        // given
        String isbn = "nonexistent-isbn";
        given(bookService.getBookByIsbn(isbn))
                .willThrow(new BookException(BookExceptionCode.BOOK_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/books/{isbn}", isbn)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Book not found"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].code").value("BOOK_NOT_FOUND"))
                .andExpect(jsonPath("$.data[0].message").value("Book not found"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(bookService).getBookByIsbn(isbn);
    }

    @Test
    @DisplayName("빈 ISBN으로 조회")
    void getBookDetail_EmptyIsbn() throws Exception {
        // given - 공백만 있는 ISBN (컨트롤러에서 validation하므로 service mock 불필요)
        String isbn = "   ";

        // when & then
        mockMvc.perform(get("/api/books/{isbn}", isbn)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest()) // Controller validation에서 400 처리
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));
    }
}