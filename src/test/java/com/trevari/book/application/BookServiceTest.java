package com.trevari.book.application;

import com.trevari.book.domain.Book;
import com.trevari.book.domain.BookRepository;
import com.trevari.book.domain.PublicationInfo;
import com.trevari.book.exception.BookException;
import com.trevari.book.exception.BookExceptionCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService 단위 테스트")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private Book sampleBook;

    @BeforeEach
    void setUp() {
        sampleBook = Book.builder()
                .isbn("9781617297397")
                .title("Java in Action")
                .subtitle("Lambdas, streams, functional and reactive programming")
                .publicationInfo(PublicationInfo.builder()
                        .authors(List.of("Raoul-Gabriel Urma", "Mario Fusco", "Alan Mycroft"))
                        .publisher("Manning Publications")
                        .publishedDate(LocalDate.of(2020, 1, 1))
                        .build())
                .build();
    }

    @Test
    @DisplayName("ISBN으로 도서 조회 성공")
    void getBookByIsbn_Success() {
        // given
        String isbn = "9781617297397";
        given(bookRepository.findByIsbn(isbn)).willReturn(Optional.of(sampleBook));

        // when
        Book result = bookService.getBookByIsbn(isbn);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getIsbn()).isEqualTo(isbn);
        assertThat(result.getTitle()).isEqualTo("Java in Action");
        assertThat(result.getSubtitle()).isEqualTo("Lambdas, streams, functional and reactive programming");
        assertThat(result.getPublicationInfo().getAuthors()).containsExactly("Raoul-Gabriel Urma", "Mario Fusco", "Alan Mycroft");
        assertThat(result.getPublicationInfo().getPublisher()).isEqualTo("Manning Publications");
        assertThat(result.getPublicationInfo().getPublishedDate()).isEqualTo(LocalDate.of(2020, 1, 1));

        verify(bookRepository).findByIsbn(isbn);
    }

    @Test
    @DisplayName("존재하지 않는 ISBN으로 조회시 BookException 발생")
    void getBookByIsbn_BookNotFound() {
        // given
        String isbn = "nonexistent-isbn";
        given(bookRepository.findByIsbn(isbn)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bookService.getBookByIsbn(isbn))
                .isInstanceOf(BookException.class)
                .extracting("exceptionCode")
                .isEqualTo(BookExceptionCode.BOOK_NOT_FOUND);

        verify(bookRepository).findByIsbn(isbn);
    }

    @Test
    @DisplayName("null ISBN으로 조회시 repository 호출")
    void getBookByIsbn_NullIsbn() {
        // given
        given(bookRepository.findByIsbn(null)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bookService.getBookByIsbn(null))
                .isInstanceOf(BookException.class)
                .extracting("exceptionCode")
                .isEqualTo(BookExceptionCode.BOOK_NOT_FOUND);

        verify(bookRepository).findByIsbn(null);
    }
}