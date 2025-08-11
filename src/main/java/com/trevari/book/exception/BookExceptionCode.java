package com.trevari.book.exception;

import com.trevari.global.exception.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum BookExceptionCode implements ExceptionCode {
    BOOK_NOT_FOUND("Book not found", HttpStatus.NOT_FOUND),
    INVALID_SEARCH_KEYWORD("Invalid search keyword", HttpStatus.BAD_REQUEST),
    INVALID_PAGE_PARAMETER("Invalid page parameter", HttpStatus.BAD_REQUEST);
    
    private final String message;
    private final HttpStatus httpStatus;
    
    BookExceptionCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
    
    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}