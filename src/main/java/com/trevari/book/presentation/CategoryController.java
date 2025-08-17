package com.trevari.book.presentation;

import com.trevari.book.application.CategoryService;
import com.trevari.book.dto.response.CategoryResponse;
import com.trevari.book.dto.response.PopularCategoryResponse;
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
public class CategoryController implements CategoryApi {

    private final CategoryService categoryService;

    @Override
    @GetMapping
    @RateLimit(limit = 200, window = 1)
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        log.info("Request to get all categories");
        
        List<CategoryResponse> categories = categoryService.getAllCategories();
        
        return ApiResponse.ok(categories, "Categories retrieved successfully");
    }
    
    @Override
    @GetMapping("/popular")
    @RateLimit(limit = 200, window = 1)
    public ResponseEntity<ApiResponse<List<PopularCategoryResponse>>> getPopularCategories(
            @RequestParam(defaultValue = "15") int limit) {
        log.info("Request to get popular categories - limit: {}", limit);
        
        List<PopularCategoryResponse> categories = categoryService.getPopularCategories(limit);
        
        return ApiResponse.ok(categories, "Popular categories retrieved successfully");
    }
}