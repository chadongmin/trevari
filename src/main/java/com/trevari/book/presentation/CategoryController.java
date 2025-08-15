package com.trevari.book.presentation;

import com.trevari.book.application.CategoryService;
import com.trevari.book.dto.response.CategoryResponse;
import com.trevari.global.dto.ApiResponse;
import com.trevari.global.ratelimit.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 카테고리 관련 API를 제공하는 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "카테고리 관리 API")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 모든 카테고리 목록 조회
     *
     * @return 카테고리 목록
     */
    @GetMapping
    @RateLimit(limit = 200, window = 1) // 200 requests per minute per IP
    @Operation(summary = "모든 카테고리 조회", description = "시스템에 등록된 모든 카테고리 목록을 반환합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "카테고리 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        log.info("Request to get all categories");
        
        List<CategoryResponse> categories = categoryService.getAllCategories();
        
        return ApiResponse.ok(categories, "Categories retrieved successfully");
    }
    
    /**
     * 인기 카테고리 목록 조회 (책 수가 많은 순)
     *
     * @param limit 조회할 카테고리 수 (기본값: 15)
     * @return 인기 카테고리 목록
     */
    @GetMapping("/popular")
    @RateLimit(limit = 200, window = 1) // 200 requests per minute per IP
    @Operation(summary = "인기 카테고리 조회", description = "책 수가 많은 순으로 정렬된 인기 카테고리 목록을 반환합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인기 카테고리 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getPopularCategories(
            @RequestParam(defaultValue = "15") int limit) {
        log.info("Request to get popular categories - limit: {}", limit);
        
        List<CategoryResponse> categories = categoryService.getPopularCategories(limit);
        
        return ApiResponse.ok(categories, "Popular categories retrieved successfully");
    }
}