package com.trevari.global.dto;


import com.trevari.global.exception.ExceptionCode;

/**
 * 에러 상세 정보
 */
public record ErrorDetail(
        String code,
        String message,
        String field
) {
    public static ErrorDetail of(ExceptionCode exceptionCode) {
        return new ErrorDetail(
                exceptionCode.getClass().getSimpleName(),
                exceptionCode.getMessage(),
                null
        );
    }

    public static ErrorDetail of(String code, String message, String field) {
        return new ErrorDetail(code, message, field);
    }
}