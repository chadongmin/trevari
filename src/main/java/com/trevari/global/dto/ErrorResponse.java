package com.trevari.global.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 에러 응답 전용 객체
 * 에러 상세 정보를 포함합니다.
 */
public record ErrorResponse(
        boolean success,
        int code,
        String error,
        String message,
        String path,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {
    public static ErrorResponse of(int code, String error, String message, String path) {
        return new ErrorResponse(
                false,
                code,
                error,
                message,
                path,
                LocalDateTime.now()
        );
    }
}