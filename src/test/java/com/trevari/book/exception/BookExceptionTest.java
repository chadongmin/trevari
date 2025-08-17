package com.trevari.book.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * BookException 및 BookExceptionCode 테스트
 */
@DisplayName("BookException 및 BookExceptionCode 테스트")
class BookExceptionTest {

    @Test
    @DisplayName("BookException 생성 테스트 - ExceptionCode 사용")
    void createBookExceptionWithExceptionCode() {
        // Given
        BookExceptionCode code = BookExceptionCode.BOOK_NOT_FOUND;

        // When
        BookException exception = new BookException(code);

        // Then
        assertThat(exception.getMessage()).isEqualTo(code.getMessage());
        assertThat(exception.getExceptionCode()).isEqualTo(code);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("BookException 기본 생성 테스트")
    void createBookExceptionBasic() {
        // Given
        BookExceptionCode code = BookExceptionCode.INVALID_SEARCH_KEYWORD;

        // When
        BookException exception = new BookException(code);

        // Then
        assertThat(exception.getMessage()).isEqualTo(code.getMessage());
        assertThat(exception.getExceptionCode()).isEqualTo(code);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("BookExceptionCode 값 검증 테스트")
    void testBookExceptionCodeValues() {
        // Given & When & Then
        // BOOK_NOT_FOUND 검증
        assertThat(BookExceptionCode.BOOK_NOT_FOUND.name()).isEqualTo("BOOK_NOT_FOUND");
        assertThat(BookExceptionCode.BOOK_NOT_FOUND.getMessage()).isEqualTo("Book not found");

        // INVALID_SEARCH_KEYWORD 검증
        assertThat(BookExceptionCode.INVALID_SEARCH_KEYWORD.name()).isEqualTo("INVALID_SEARCH_KEYWORD");
        assertThat(BookExceptionCode.INVALID_SEARCH_KEYWORD.getMessage()).isEqualTo("Invalid search keyword");

        // INVALID_PAGE_PARAMETER 검증
        assertThat(BookExceptionCode.INVALID_PAGE_PARAMETER.name()).isEqualTo("INVALID_PAGE_PARAMETER");
        assertThat(BookExceptionCode.INVALID_PAGE_PARAMETER.getMessage()).isEqualTo("Invalid page parameter");
    }

    @Test
    @DisplayName("BookExceptionCode enum 특성 테스트")
    void testBookExceptionCodeEnumProperties() {
        // Given
        BookExceptionCode[] codes = BookExceptionCode.values();

        // When & Then
        assertThat(codes).hasSize(3); // 현재 정의된 예외 코드 수

        // 모든 코드에 대해 name과 message가 null이 아님을 확인
        for (BookExceptionCode code : codes) {
            assertThat(code.name()).isNotNull();
            assertThat(code.name()).isNotEmpty();
            assertThat(code.getMessage()).isNotNull();
            assertThat(code.getMessage()).isNotEmpty();
        }

        // valueOf 테스트
        assertThat(BookExceptionCode.valueOf("BOOK_NOT_FOUND")).isEqualTo(BookExceptionCode.BOOK_NOT_FOUND);
    }

    @Test
    @DisplayName("BookException 메시지 전파 테스트")
    void testBookExceptionMessagePropagation() {
        // Given
        BookExceptionCode code = BookExceptionCode.BOOK_NOT_FOUND;

        // When
        BookException exception = new BookException(code);

        // Then
        assertThat(exception.getMessage()).contains("Book not found");
        assertThat(exception.toString()).contains("BookException");
        assertThat(exception.toString()).contains("Book not found");
    }

    @Test
    @DisplayName("BookException 스택 트레이스 테스트")
    void testBookExceptionStackTrace() {
        // Given
        BookExceptionCode code = BookExceptionCode.INVALID_SEARCH_KEYWORD;

        // When
        BookException exception = new BookException(code);

        // Then
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertThat(stackTrace).isNotEmpty();
        assertThat(stackTrace[0].getMethodName()).isEqualTo("testBookExceptionStackTrace");
    }

    @Test
    @DisplayName("BookException HttpStatus 테스트")
    void testBookExceptionHttpStatus() {
        // Given
        BookException notFoundException = new BookException(BookExceptionCode.BOOK_NOT_FOUND);
        BookException badRequestException = new BookException(BookExceptionCode.INVALID_SEARCH_KEYWORD);

        // When & Then
        assertThat(notFoundException.getExceptionCode().getHttpStatus()).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
        assertThat(badRequestException.getExceptionCode().getHttpStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("BookException 다양한 시나리오 테스트")
    void testBookExceptionVariousScenarios() {
        // 시나리오 1: 책을 찾을 수 없는 경우
        BookException notFoundException = new BookException(BookExceptionCode.BOOK_NOT_FOUND);
        assertThat(((BookExceptionCode) notFoundException.getExceptionCode()).name()).isEqualTo("BOOK_NOT_FOUND");

        // 시나리오 2: 검색어가 유효하지 않은 경우
        BookException invalidKeywordException = new BookException(BookExceptionCode.INVALID_SEARCH_KEYWORD);
        assertThat(((BookExceptionCode) invalidKeywordException.getExceptionCode()).name()).isEqualTo("INVALID_SEARCH_KEYWORD");

        // 시나리오 3: 페이지 파라미터가 유효하지 않은 경우
        BookException invalidPageException = new BookException(BookExceptionCode.INVALID_PAGE_PARAMETER);
        assertThat(((BookExceptionCode) invalidPageException.getExceptionCode()).name()).isEqualTo("INVALID_PAGE_PARAMETER");
    }

    @Test
    @DisplayName("BookException 직렬화 가능성 테스트")
    void testBookExceptionSerializability() {
        // Given
        BookException exception = new BookException(BookExceptionCode.BOOK_NOT_FOUND);

        // When & Then - Exception이 Serializable을 구현하는지 확인
        assertThat(exception).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    @DisplayName("BookExceptionCode 코드 유니크성 테스트")
    void testBookExceptionCodeUniqueness() {
        // Given
        BookExceptionCode[] codes = BookExceptionCode.values();

        // When & Then - 모든 코드가 유니크한지 확인
        java.util.Set<String> codeSet = new java.util.HashSet<>();
        for (BookExceptionCode code : codes) {
            boolean added = codeSet.add(code.name());
            assertThat(added).isTrue(); // 중복된 코드가 없어야 함
        }

        assertThat(codeSet).hasSize(codes.length);
    }
}