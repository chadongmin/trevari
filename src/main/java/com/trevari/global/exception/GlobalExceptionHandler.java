package com.trevari.global.exception;

import com.trevari.book.exception.BookException;
import com.trevari.global.dto.ApiResponse;
import com.trevari.global.dto.ErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BookException.class)
    public ResponseEntity<ApiResponse<List<ErrorDetail>>> handleBookException(BookException e, HttpServletRequest request) {
        log.error("Book exception at {}: ", request.getRequestURI(), e);

        ErrorDetail errorDetail = ErrorDetail.of(e.getExceptionCode());
        List<ErrorDetail> errors = List.of(errorDetail);

        return ResponseEntity
                .status(e.getExceptionCode().getHttpStatus())
                .body(new ApiResponse<>(
                        false,
                        e.getExceptionCode().getHttpStatus().value(),
                        e.getMessage(),
                        errors,
                        java.time.LocalDateTime.now()
                ));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<List<ErrorDetail>>> handleValidationException(BindException e, HttpServletRequest request) {
        log.error("Validation error at {}: ", request.getRequestURI(), e);

        List<ErrorDetail> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> ErrorDetail.of(
                        "VALIDATION_ERROR",
                        error.getDefaultMessage(),
                        error.getField()
                ))
                .toList();

        return ResponseEntity
                .badRequest()
                .body(new ApiResponse<>(
                        false,
                        400,
                        "Validation failed",
                        errors,
                        java.time.LocalDateTime.now()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<List<ErrorDetail>>> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.error("Illegal argument at {}: ", request.getRequestURI(), e);

        ErrorDetail errorDetail = ErrorDetail.of("ILLEGAL_ARGUMENT", e.getMessage(), null);
        List<ErrorDetail> errors = List.of(errorDetail);

        return ResponseEntity
                .badRequest()
                .body(new ApiResponse<>(
                        false,
                        400,
                        "Bad Request",
                        errors,
                        java.time.LocalDateTime.now()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<List<ErrorDetail>>> handleGeneralException(Exception e, HttpServletRequest request) {
        log.error("Unexpected error at {}: ", request.getRequestURI(), e);

        ErrorDetail errorDetail = ErrorDetail.of("INTERNAL_ERROR", "An unexpected error occurred", null);
        List<ErrorDetail> errors = List.of(errorDetail);

        return ResponseEntity
                .internalServerError()
                .body(new ApiResponse<>(
                        false,
                        500,
                        "Internal Server Error",
                        errors,
                        java.time.LocalDateTime.now()
                ));
    }
}