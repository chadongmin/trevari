package com.trevari.book.application;

import com.trevari.book.domain.Category;
import com.trevari.book.dto.response.CategoryResponse;
import com.trevari.book.dto.response.PopularCategoryResponse;
import com.trevari.book.persistence.CategoryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * CategoryService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService 단위 테스트")
class CategoryServiceTest {

    @Mock
    private CategoryJpaRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private List<Category> testCategories;
    private List<Object[]> testPopularCategories;

    @BeforeEach
    void setUp() {
        // 테스트 카테고리 데이터 준비
        testCategories = Arrays.asList(
                Category.builder().id(1L).name("Programming").build(),
                Category.builder().id(2L).name("Technology").build(),
                Category.builder().id(3L).name("Science").build()
        );

        // 인기 카테고리 데이터 준비 [id, name, bookCount]
        testPopularCategories = Arrays.asList(
                new Object[]{1L, "Programming", 150L},
                new Object[]{2L, "Technology", 100L},
                new Object[]{3L, "Science", 50L}
        );
    }

    @Test
    @DisplayName("모든 카테고리 조회 테스트")
    void getAllCategories() {
        // Given
        given(categoryRepository.findAllByOrderByNameAsc()).willReturn(testCategories);

        // When
        List<CategoryResponse> result = categoryService.getAllCategories();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting(CategoryResponse::name)
                .containsExactly("Programming", "Technology", "Science");

        verify(categoryRepository).findAllByOrderByNameAsc();
    }

    @Test
    @DisplayName("빈 카테고리 목록 조회 테스트")
    void getAllCategories_EmptyList() {
        // Given
        given(categoryRepository.findAllByOrderByNameAsc()).willReturn(List.of());

        // When
        List<CategoryResponse> result = categoryService.getAllCategories();

        // Then
        assertThat(result).isEmpty();
        verify(categoryRepository).findAllByOrderByNameAsc();
    }

    @Test
    @DisplayName("인기 카테고리 조회 테스트")
    void getPopularCategories() {
        // Given
        given(categoryRepository.findCategoriesWithBookCount()).willReturn(testPopularCategories);

        // When
        List<PopularCategoryResponse> result = categoryService.getPopularCategories(3);

        // Then
        assertThat(result).hasSize(3);
        
        // 첫 번째 카테고리 검증 (가장 인기)
        PopularCategoryResponse firstCategory = result.get(0);
        assertThat(firstCategory.id()).isEqualTo(1L);
        assertThat(firstCategory.name()).isEqualTo("Programming");
        assertThat(firstCategory.bookCount()).isEqualTo(150L);

        // 두 번째 카테고리 검증
        PopularCategoryResponse secondCategory = result.get(1);
        assertThat(secondCategory.id()).isEqualTo(2L);
        assertThat(secondCategory.name()).isEqualTo("Technology");
        assertThat(secondCategory.bookCount()).isEqualTo(100L);

        verify(categoryRepository).findCategoriesWithBookCount();
    }

    @Test
    @DisplayName("인기 카테고리 조회 - 제한된 수 테스트")
    void getPopularCategories_WithLimit() {
        // Given
        given(categoryRepository.findCategoriesWithBookCount()).willReturn(testPopularCategories);

        // When
        List<PopularCategoryResponse> result = categoryService.getPopularCategories(2);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(PopularCategoryResponse::name)
                .containsExactly("Programming", "Technology");

        verify(categoryRepository).findCategoriesWithBookCount();
    }

    @Test
    @DisplayName("인기 카테고리 조회 - 빈 결과 테스트")
    void getPopularCategories_EmptyResult() {
        // Given
        given(categoryRepository.findCategoriesWithBookCount()).willReturn(List.of());

        // When
        List<PopularCategoryResponse> result = categoryService.getPopularCategories(10);

        // Then
        assertThat(result).isEmpty();
        verify(categoryRepository).findCategoriesWithBookCount();
    }

    @Test
    @DisplayName("카테고리 응답 변환 테스트")
    void testCategoryResponseConversion() {
        // Given
        Category category = Category.builder()
                .id(999L)
                .name("Test Category")
                .build();
        
        given(categoryRepository.findAllByOrderByNameAsc()).willReturn(List.of(category));

        // When
        List<CategoryResponse> result = categoryService.getAllCategories();

        // Then
        assertThat(result).hasSize(1);
        CategoryResponse response = result.get(0);
        assertThat(response.id()).isEqualTo(999L);
        assertThat(response.name()).isEqualTo("Test Category");
    }

    @Test
    @DisplayName("인기 카테고리 Number 타입 변환 테스트")
    void testPopularCategoryNumberConversion() {
        // Given - 다양한 Number 타입 테스트
        List<Object[]> mixedNumberTypes = Arrays.asList(
                new Object[]{1, "Category1", 100}, // Integer
                new Object[]{2L, "Category2", 200L}, // Long
                new Object[]{3, "Category3", 300L}  // Mixed
        );
        
        given(categoryRepository.findCategoriesWithBookCount()).willReturn(mixedNumberTypes);

        // When
        List<PopularCategoryResponse> result = categoryService.getPopularCategories(3);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).bookCount()).isEqualTo(100L);
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).bookCount()).isEqualTo(200L);
    }

    @Test
    @DisplayName("Repository 예외 발생 시 처리 테스트")
    void testRepositoryException() {
        // Given
        given(categoryRepository.findAllByOrderByNameAsc()).willThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> categoryService.getAllCategories())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

        verify(categoryRepository).findAllByOrderByNameAsc();
    }

    @Test
    @DisplayName("인기 카테고리 Repository 예외 발생 시 처리 테스트")
    void testPopularCategoriesRepositoryException() {
        // Given
        given(categoryRepository.findCategoriesWithBookCount())
                .willThrow(new RuntimeException("Database connection failed"));

        // When & Then
        assertThatThrownBy(() -> categoryService.getPopularCategories(10))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection failed");

        verify(categoryRepository).findCategoriesWithBookCount();
    }

    @Test
    @DisplayName("인기 카테고리 제한 수 0 테스트")
    void testPopularCategoriesWithZeroLimit() {
        // Given
        given(categoryRepository.findCategoriesWithBookCount()).willReturn(testPopularCategories);

        // When
        List<PopularCategoryResponse> result = categoryService.getPopularCategories(0);

        // Then
        assertThat(result).isEmpty();
        verify(categoryRepository).findCategoriesWithBookCount();
    }

    @Test
    @DisplayName("인기 카테고리 제한 수가 데이터보다 클 때 테스트")
    void testPopularCategoriesWithLargeLimit() {
        // Given
        given(categoryRepository.findCategoriesWithBookCount()).willReturn(testPopularCategories);

        // When
        List<PopularCategoryResponse> result = categoryService.getPopularCategories(100);

        // Then
        assertThat(result).hasSize(3); // 실제 데이터 수만큼만 반환
        verify(categoryRepository).findCategoriesWithBookCount();
    }
}