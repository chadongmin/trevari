package com.trevari.book.presentation;

import com.trevari.book.dto.response.BookResponse;
import com.trevari.book.dto.response.BookSearchResponse;
import com.trevari.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Book API", description = "도서 조회 및 검색 API")
public interface BookApi {
    
    @Operation(summary = "도서 상세 조회", description = "ISBN으로 특정 도서의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "도서 조회 성공",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "도서를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "서버 내부 오류",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    ResponseEntity<ApiResponse<BookResponse>> getBookDetail(
        @Parameter(description = "도서 ISBN", required = true, example = "9781617297397")
        @PathVariable String isbn);
    
    @Operation(summary = "도서 검색", description = "키워드로 도서를 검색합니다. OR(|), NOT(-) 연산자를 지원합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "검색 성공",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 검색 키워드",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    ResponseEntity<ApiResponse<BookSearchResponse>> searchBooks(
        @Parameter(description = "검색 키워드 (OR: keyword1|keyword2, NOT: keyword1 -keyword2)", required = true, example = "Java")
        @RequestParam String keyword,
        @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
        @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20") int size);
}
