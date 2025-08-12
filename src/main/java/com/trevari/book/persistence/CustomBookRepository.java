package com.trevari.book.persistence;

import com.trevari.book.domain.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 복잡한 쿼리를 위한 커스텀 Repository 인터페이스
 */
public interface CustomBookRepository {
    
    /**
     * 키워드로 도서 검색 (제목, 부제목, 저자명 대상)
     */
    Page<Book> findByKeyword(String keyword, Pageable pageable);
    
    /**
     * OR 연산 키워드 검색
     */
    Page<Book> findByOrKeywords(String keyword1, String keyword2, Pageable pageable);
    
    /**
     * NOT 연산 키워드 검색 (첫 번째 키워드 포함, 두 번째 키워드 제외)
     */
    Page<Book> findByNotKeywords(String includeKeyword, String excludeKeyword, Pageable pageable);
}
