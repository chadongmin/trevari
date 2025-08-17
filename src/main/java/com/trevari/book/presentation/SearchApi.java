package com.trevari.book.presentation;

import com.trevari.book.dto.response.BookSearchResponse;
import com.trevari.book.dto.response.PopularSearchResponse;
import com.trevari.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Search API", description = "도서 검색 및 인기 키워드 API")
public interface SearchApi {
    
    @Operation(
        summary = "도서 검색", 
        description = "키워드로 도서를 검색합니다. OR(|) 연산자와 NOT(-) 연산자를 지원하며, " +
                     "제목, 부제목, 저자 필드를 대상으로 검색합니다. 최대 2개의 키워드를 지원합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "검색 성공 - 검색 결과와 실행 시간, 검색 전략 메타데이터 포함",
            content = @Content(schema = @Schema(implementation = BookSearchResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 검색 쿼리 - 빈 키워드 또는 잘못된 형식",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429",
            description = "요청 한도 초과 - 10초 동안 3회 제한",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    ResponseEntity<ApiResponse<BookSearchResponse>> searchBooks(
        @Parameter(
            description = "검색 키워드. OR 연산: 'Java|Spring', NOT 연산: 'Java -Spring'", 
            required = true, 
            example = "Spring Boot"
        )
        @RequestParam String keyword,
        
        @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
        @RequestParam(defaultValue = "1") int page,
        
        @Parameter(description = "페이지 크기 (1-100)", example = "20")
        @RequestParam(defaultValue = "20") int size
    );
    
    @Operation(
        summary = "인기 검색 키워드 조회", 
        description = "검색 횟수 기준으로 상위 10개의 인기 키워드를 조회합니다. " +
                     "실시간 Redis 데이터를 기반으로 합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "인기 키워드 조회 성공 - 키워드와 검색 횟수 포함",
            content = @Content(schema = @Schema(implementation = PopularSearchResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429",
            description = "요청 한도 초과 - 1분 동안 20회 제한",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    ResponseEntity<ApiResponse<PopularSearchResponse>> getPopularKeywords();
}