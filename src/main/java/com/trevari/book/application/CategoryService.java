package com.trevari.book.application;

import com.trevari.book.domain.Category;
import com.trevari.book.dto.response.CategoryResponse;
import com.trevari.book.dto.response.PopularCategoryResponse;
import com.trevari.book.persistence.CategoryJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 카테고리 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryJpaRepository categoryRepository;

    /**
     * 모든 카테고리 조회
     *
     * @return 카테고리 목록
     */
    public List<CategoryResponse> getAllCategories() {
        log.debug("Fetching all categories");
        
        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();
        
        return categories.stream()
                .map(CategoryResponse::from)
                .toList();
    }
    
    /**
     * 인기 카테고리 조회 (책 수가 많은 순)
     *
     * @param limit 조회할 카테고리 수
     * @return 인기 카테고리 목록 (책 수 포함)
     */
    public List<PopularCategoryResponse> getPopularCategories(int limit) {
        log.debug("Fetching popular categories with limit: {}", limit);
        
        List<Object[]> categoriesWithCount = categoryRepository.findCategoriesWithBookCount();
        
        return categoriesWithCount.stream()
                .limit(limit)
                .map(row -> {
                    Long id = ((Number) row[0]).longValue();
                    String name = (String) row[1];
                    Long bookCount = ((Number) row[2]).longValue();
                    return PopularCategoryResponse.of(id, name, bookCount);
                })
                .toList();
    }
}