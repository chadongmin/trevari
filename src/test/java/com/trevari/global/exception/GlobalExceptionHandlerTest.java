package com.trevari.global.exception;

import com.trevari.book.exception.BookException;
import com.trevari.book.exception.BookExceptionCode;
import com.trevari.global.dto.ApiResponse;
import com.trevari.global.dto.ErrorDetail;
import com.trevari.global.ratelimit.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private HttpServletRequest httpServletRequest;

    @BeforeEach
    void setUp() {
        when(httpServletRequest.getRequestURI()).thenReturn("/api/books");
    }

    @Test
    @DisplayName("BookException 처리 테스트")
    void handleBookException() {
        // Given
        BookException bookException = new BookException(BookExceptionCode.BOOK_NOT_FOUND);

        // When
        ResponseEntity<ApiResponse<List<ErrorDetail>>> response = 
            globalExceptionHandler.handleBookException(bookException, httpServletRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().code()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Book not found");
        assertThat(response.getBody().data()).hasSize(1);
        assertThat(response.getBody().data().get(0).code()).isEqualTo("BOOK_NOT_FOUND");
    }

    @Test
    @DisplayName("IllegalArgumentException 처리 테스트")
    void handleIllegalArgumentException() {
        // Given
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("Invalid parameter");

        // When
        ResponseEntity<ApiResponse<List<ErrorDetail>>> response = 
            globalExceptionHandler.handleIllegalArgumentException(illegalArgumentException, httpServletRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().code()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Bad Request");
        assertThat(response.getBody().data()).hasSize(1);
        assertThat(response.getBody().data().get(0).code()).isEqualTo("ILLEGAL_ARGUMENT");
        assertThat(response.getBody().data().get(0).message()).isEqualTo("Invalid parameter");
    }

    @Test
    @DisplayName("일반 Exception 처리 테스트")
    void handleGeneralException() {
        // Given
        RuntimeException runtimeException = new RuntimeException("Unexpected error");

        // When
        ResponseEntity<ApiResponse<List<ErrorDetail>>> response = 
            globalExceptionHandler.handleGeneralException(runtimeException, httpServletRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().code()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().data()).hasSize(1);
        assertThat(response.getBody().data().get(0).code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().data().get(0).message()).isEqualTo("An unexpected error occurred");
    }

    @Test
    @DisplayName("BindException 처리 테스트")
    void handleValidationException() {
        // Given
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "testObject");
        bindingResult.addError(new FieldError("testObject", "name", "Name is required"));
        bindingResult.addError(new FieldError("testObject", "age", "Age must be positive"));
        
        BindException bindException = new BindException(bindingResult);

        // When
        ResponseEntity<ApiResponse<List<ErrorDetail>>> response = 
            globalExceptionHandler.handleValidationException(bindException, httpServletRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().code()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
        assertThat(response.getBody().data()).hasSize(2);
        
        // 순서에 관계없이 검증
        List<String> errorMessages = response.getBody().data().stream()
            .map(ErrorDetail::message)
            .toList();
        assertThat(errorMessages).containsExactlyInAnyOrder("Name is required", "Age must be positive");
    }

    @Test
    @DisplayName("RateLimitExceededException 처리 테스트")
    void handleRateLimitExceeded() {
        // Given
        RateLimitExceededException rateLimitException = new RateLimitExceededException(
            10,
            60L,
            30L
        );

        // When
        ResponseEntity<ApiResponse<List<ErrorDetail>>> response = 
            globalExceptionHandler.handleRateLimitExceeded(rateLimitException, httpServletRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getHeaders().getFirst("X-RateLimit-Window")).isEqualTo("60");
        assertThat(response.getHeaders().getFirst("X-RateLimit-Retry-After")).isEqualTo("30");
        
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().code()).isEqualTo(429);
        assertThat(response.getBody().message()).isEqualTo("Rate limit exceeded");
        assertThat(response.getBody().data()).hasSize(1);
        assertThat(response.getBody().data().get(0).code()).isEqualTo("RATE_LIMIT_EXCEEDED");
    }
}