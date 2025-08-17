package com.trevari.book.persistence;

import com.trevari.book.domain.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 성능 최적화된 검색 쿼리 (풀텍스트 전용)
 * 주로 MySQL 풀텍스트 검색 지원용, QueryDSL이 메인
 */
public interface OptimizedBookRepository extends JpaRepository<Book, String> {

    /**
     * MySQL 풀텍스트 검색 사용 (최고 성능)
     * 인덱스가 없으면 실패하므로 fallback 필요
     */
    @Query(value = """
            (SELECT b.*, MATCH(b.title, b.subtitle) AGAINST(:keyword IN NATURAL LANGUAGE MODE) as relevance_score
             FROM book b 
             WHERE MATCH(b.title, b.subtitle) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
             ORDER BY relevance_score DESC)
            UNION
            (SELECT DISTINCT b.*, MATCH(a.name) AGAINST(:keyword IN NATURAL LANGUAGE MODE) as relevance_score
             FROM book b 
             JOIN book_author ba ON b.isbn = ba.book_isbn 
             JOIN author a ON ba.author_id = a.id
             WHERE MATCH(a.name) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
             ORDER BY relevance_score DESC)
            ORDER BY relevance_score DESC
            """,
            countQuery = """
                    SELECT COUNT(DISTINCT b.isbn) FROM book b
                    LEFT JOIN book_author ba ON b.isbn = ba.book_isbn
                    LEFT JOIN author a ON ba.author_id = a.id
                    WHERE MATCH(b.title, b.subtitle) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
                       OR MATCH(a.name) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
                    """,
            nativeQuery = true)
    Page<Book> findByFullTextSearch(@Param("keyword") String keyword, Pageable pageable);

    /**
     * OR 검색 풀텍스트 최적화
     */
    @Query(value = """
            (SELECT b.*, MATCH(b.title, b.subtitle) AGAINST(:keyword1 IN NATURAL LANGUAGE MODE) as relevance_score
             FROM book b 
             WHERE MATCH(b.title, b.subtitle) AGAINST(:keyword1 IN NATURAL LANGUAGE MODE))
            UNION
            (SELECT b.*, MATCH(b.title, b.subtitle) AGAINST(:keyword2 IN NATURAL LANGUAGE MODE) as relevance_score
             FROM book b 
             WHERE MATCH(b.title, b.subtitle) AGAINST(:keyword2 IN NATURAL LANGUAGE MODE))
            UNION
            (SELECT DISTINCT b.*, MATCH(a.name) AGAINST(:keyword1 IN NATURAL LANGUAGE MODE) as relevance_score
             FROM book b 
             JOIN book_author ba ON b.isbn = ba.book_isbn 
             JOIN author a ON ba.author_id = a.id
             WHERE MATCH(a.name) AGAINST(:keyword1 IN NATURAL LANGUAGE MODE))
            UNION
            (SELECT DISTINCT b.*, MATCH(a.name) AGAINST(:keyword2 IN NATURAL LANGUAGE MODE) as relevance_score
             FROM book b 
             JOIN book_author ba ON b.isbn = ba.book_isbn 
             JOIN author a ON ba.author_id = a.id
             WHERE MATCH(a.name) AGAINST(:keyword2 IN NATURAL LANGUAGE MODE))
            ORDER BY relevance_score DESC
            """,
            countQuery = """
                    SELECT COUNT(DISTINCT b.isbn) FROM book b
                    LEFT JOIN book_author ba ON b.isbn = ba.book_isbn
                    LEFT JOIN author a ON ba.author_id = a.id
                    WHERE MATCH(b.title, b.subtitle) AGAINST(:keyword1 IN NATURAL LANGUAGE MODE)
                       OR MATCH(b.title, b.subtitle) AGAINST(:keyword2 IN NATURAL LANGUAGE MODE)
                       OR MATCH(a.name) AGAINST(:keyword1 IN NATURAL LANGUAGE MODE)
                       OR MATCH(a.name) AGAINST(:keyword2 IN NATURAL LANGUAGE MODE)
                    """,
            nativeQuery = true)
    Page<Book> findByOrFullTextSearch(@Param("keyword1") String keyword1,
                                      @Param("keyword2") String keyword2,
                                      Pageable pageable);

    /**
     * NOT 검색 풀텍스트 최적화
     * includeKeyword는 포함하고 excludeKeyword는 제외
     */
    @Query(value = """
            SELECT DISTINCT result.* FROM (
                (SELECT b.*, MATCH(b.title, b.subtitle) AGAINST(:includeKeyword IN NATURAL LANGUAGE MODE) as relevance_score
                 FROM book b 
                 WHERE MATCH(b.title, b.subtitle) AGAINST(:includeKeyword IN NATURAL LANGUAGE MODE)
                   AND NOT MATCH(b.title, b.subtitle) AGAINST(:excludeKeyword IN NATURAL LANGUAGE MODE))
                UNION
                (SELECT DISTINCT b.*, MATCH(a.name) AGAINST(:includeKeyword IN NATURAL LANGUAGE MODE) as relevance_score
                 FROM book b 
                 JOIN book_author ba ON b.isbn = ba.book_isbn 
                 JOIN author a ON ba.author_id = a.id
                 WHERE MATCH(a.name) AGAINST(:includeKeyword IN NATURAL LANGUAGE MODE)
                   AND NOT (MATCH(b.title, b.subtitle) AGAINST(:excludeKeyword IN NATURAL LANGUAGE MODE)
                            OR MATCH(a.name) AGAINST(:excludeKeyword IN NATURAL LANGUAGE MODE)))
            ) result
            ORDER BY result.relevance_score DESC
            """,
            countQuery = """
                    SELECT COUNT(DISTINCT b.isbn) FROM book b
                    LEFT JOIN book_author ba ON b.isbn = ba.book_isbn
                    LEFT JOIN author a ON ba.author_id = a.id
                    WHERE (MATCH(b.title, b.subtitle) AGAINST(:includeKeyword IN NATURAL LANGUAGE MODE)
                           OR MATCH(a.name) AGAINST(:includeKeyword IN NATURAL LANGUAGE MODE))
                      AND NOT (MATCH(b.title, b.subtitle) AGAINST(:excludeKeyword IN NATURAL LANGUAGE MODE)
                               OR MATCH(a.name) AGAINST(:excludeKeyword IN NATURAL LANGUAGE MODE))
                    """,
            nativeQuery = true)
    Page<Book> findByNotFullTextSearch(@Param("includeKeyword") String includeKeyword,
                                       @Param("excludeKeyword") String excludeKeyword,
                                       Pageable pageable);
}