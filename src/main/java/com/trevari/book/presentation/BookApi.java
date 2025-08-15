package com.trevari.book.presentation;

import com.trevari.book.dto.response.DetailedBookResponse;
import com.trevari.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Book API", description = "도서 조회 및 검색 API")
public interface BookApi {
    
    @Operation(summary = "도서 상세 조회", description = "ISBN으로 특정 도서의 완전한 상세 정보를 조회합니다. 모든 도메인 필드를 포함합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "도서 상세 정보 조회 성공",
            content = @Content(schema = @Schema(implementation = DetailedBookResponse.class))
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
    ResponseEntity<ApiResponse<DetailedBookResponse>> getBookDetail(
        @Parameter(description = "도서 ISBN", required = true, example = "9781617297397")
        @PathVariable String isbn);
}
