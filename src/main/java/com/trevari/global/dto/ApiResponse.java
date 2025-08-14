package com.trevari.global.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

/**
 * API 공통 응답 객체
 * 성공/실패에 대한 일관된 응답 구조를 제공합니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "API 공통 응답")
public record ApiResponse<T>(
        @Schema(description = "성공 여부", example = "true")
        boolean success,
        @Schema(description = "응답 코드", example = "200")
        int code,
        @Schema(description = "응답 메시지", example = "Book retrieved successfully")
        String message,
        @Schema(description = "응답 데이터")
        T data,
        @Schema(description = "응답 시간", example = "2023-12-01T10:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {
    // 성공 응답 생성
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                HttpStatus.OK.value(),
                "Success",
                data,
                LocalDateTime.now()
        ));
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                HttpStatus.OK.value(),
                message,
                data,
                LocalDateTime.now()
        ));
    }

    // 성공 응답 (데이터 없음)
    public static ResponseEntity<ApiResponse<Void>> ok() {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                HttpStatus.OK.value(),
                "Success",
                null,
                LocalDateTime.now()
        ));
    }

    // 생성 성공 응답
    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        HttpStatus.CREATED.value(),
                        "Created",
                        data,
                        LocalDateTime.now()
                ));
    }

    // 실패 응답 생성
    public static ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(
                        false,
                        status.value(),
                        message,
                        null,
                        LocalDateTime.now()
                ));
    }

    public static ResponseEntity<ApiResponse<Void>> badRequest(String message) {
        return error(HttpStatus.BAD_REQUEST, message);
    }

    public static ResponseEntity<ApiResponse<Void>> notFound(String message) {
        return error(HttpStatus.NOT_FOUND, message);
    }

    public static ResponseEntity<ApiResponse<Void>> internalServerError(String message) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}