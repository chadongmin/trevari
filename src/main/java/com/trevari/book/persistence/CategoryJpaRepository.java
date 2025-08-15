package com.trevari.book.persistence;

import com.trevari.book.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Category JPA Repository
 */
@Repository
public interface CategoryJpaRepository extends JpaRepository<Category, Long> {
    
    /**
     * 모든 카테고리를 이름 순으로 정렬하여 조회
     *
     * @return 이름 순으로 정렬된 카테고리 목록
     */
    List<Category> findAllByOrderByNameAsc();
    
    /**
     * 카테고리별 책 수와 함께 조회 (책 수가 많은 순으로 정렬)
     *
     * @return 카테고리 ID, 이름, 책 수를 포함한 Object 배열 목록
     */
    @Query(value = "SELECT c.id, c.name, COUNT(bc.book_isbn) as book_count " +
                   "FROM category c LEFT JOIN book_category bc ON c.id = bc.category_id " +
                   "GROUP BY c.id, c.name " +
                   "ORDER BY COUNT(bc.book_isbn) DESC, c.name ASC", 
           nativeQuery = true)
    List<Object[]> findCategoriesWithBookCount();
}