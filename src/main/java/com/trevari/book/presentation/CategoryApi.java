package com.trevari.book.presentation;

import com.trevari.book.dto.response.CategoryResponse;
import com.trevari.book.dto.response.PopularCategoryResponse;
import com.trevari.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Category API", description = "도서 카테고리 관리 및 조회 API")
public interface CategoryApi {
    
    @Operation(
        summary = "전체 카테고리 목록 조회", 
        description = "시스템에 등록된 모든 도서 카테고리 목록을 조회합니다. " +
                     "카테고리명과 각 카테고리별 도서 수를 포함합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "카테고리 목록 조회 성공 - 모든 카테고리 정보 반환",
            content = @Content(schema = @Schema(implementation = CategoryResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429",
            description = "요청 한도 초과 - 1분 동안 200회 제한",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500", 
            description = "서버 내부 오류",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories();
    
    @Operation(
        summary = "인기 카테고리 목록 조회", 
        description = "도서 수가 많은 순으로 정렬된 인기 카테고리 목록을 조회합니다. " +
                     "각 카테고리의 도서 수와 함께 반환됩니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "인기 카테고리 목록 조회 성공 - 도서 수 기준 상위 카테고리 반환",
            content = @Content(schema = @Schema(implementation = PopularCategoryResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 - limit 값이 유효하지 않음",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429",
            description = "요청 한도 초과 - 1분 동안 200회 제한",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500", 
            description = "서버 내부 오류",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    ResponseEntity<ApiResponse<List<PopularCategoryResponse>>> getPopularCategories(
        @Parameter(
            description = "조회할 인기 카테고리 수 (1-50)", 
            example = "15"
        )
        @RequestParam(defaultValue = "15") int limit
    );
}