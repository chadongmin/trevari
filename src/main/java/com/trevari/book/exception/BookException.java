package com.trevari.book.exception;

import com.trevari.global.exception.ExceptionCode;
import lombok.Getter;

@Getter
public class BookException extends RuntimeException {
    private final ExceptionCode exceptionCode;
    
    public BookException(ExceptionCode exceptionCode) {
        super(exceptionCode.getMessage());
        this.exceptionCode = exceptionCode;
    }
}