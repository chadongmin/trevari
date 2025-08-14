package com.trevari.book.application;

import com.trevari.book.domain.Book;
import com.trevari.book.dto.response.BookSearchResponse;
import com.trevari.book.dto.response.CacheableBookSearchResult;
import com.trevari.book.exception.BookException;
import com.trevari.book.exception.BookExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 도서 비즈니스 로직을 담당하는 서비스 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {
    
    private final SearchKeywordService searchKeywordService;
    private final BookCacheService bookCacheService;
    
    /**
     * ISBN으로 도서 단건 조회
     *
     * @param isbn 도서 ISBN
     * @return 조회된 도서 정보
     * @throws BookException 도서를 찾을 수 없는 경우
     */
    public Book getBookByIsbn(String isbn) {
        log.debug("Finding book by ISBN: {}", isbn);
        
        Book book = bookCacheService.getCachedBookByIsbn(isbn);
        if (book == null) {
            log.warn("Book not found with ISBN: {}", isbn);
            throw new BookException(BookExceptionCode.BOOK_NOT_FOUND);
        }
        
        return book;
    }
    
    /**
     * 키워드로 도서 검색 (캐싱 최적화 버전)
     *
     * @param keyword  검색 키워드
     * @param pageable 페이징 정보
     * @return 검색 결과
     */
    public BookSearchResponse searchBooks(String keyword, Pageable pageable) {
        log.info("Searching books with keyword: {}, page: {}, size: {}",
            keyword, pageable.getPageNumber(), pageable.getPageSize());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 캐시된 결과 조회 (execution time 제외)
            CacheableBookSearchResult cachedResult = bookCacheService.getCachedSearchResult(keyword, pageable);
            
            // 검색 키워드 기록 - Redis 방식 사용 (동시성 문제 해결)
            searchKeywordService.recordSearchKeywordWithRedis(keyword);
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Book search completed in {}ms, found {} books",
                executionTime, cachedResult.getBooks().size());
            
            // 캐시된 결과에 새로운 execution time을 추가하여 응답 생성
            return cachedResult.toResponse(executionTime);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid search query: {}", keyword, e);
            throw new BookException(BookExceptionCode.INVALID_SEARCH_KEYWORD);
        }
    }

    /**
     * 전체 도서 목록 조회 (페이징)
     *
     * @param pageable 페이징 정보
     * @return 전체 도서 목록
     */
    public BookSearchResponse getAllBooks(Pageable pageable) {
        log.info("Getting all books - page: {}, size: {}",
            pageable.getPageNumber(), pageable.getPageSize());
        
        long startTime = System.currentTimeMillis();
        
        // 전체 도서를 빈 문자열로 검색하여 캐시 활용
        CacheableBookSearchResult cachedResult = bookCacheService.getAllBooksCached(pageable);
        
        long executionTime = System.currentTimeMillis() - startTime;
        log.info("All books retrieval completed in {}ms, found {} books",
            executionTime, cachedResult.getBooks().size());
        
        return cachedResult.toResponse(executionTime);
    }
}