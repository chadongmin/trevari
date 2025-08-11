package com.trevari.global.exception;

import org.springframework.http.HttpStatus;

public interface ExceptionCode {
    String getMessage();
    HttpStatus getHttpStatus();
}