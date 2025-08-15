package com.trevari.book.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trevari.book.domain.Book;
import com.trevari.book.domain.BookRepository;
import com.trevari.book.domain.search.SearchQuery;
import com.trevari.book.domain.search.SearchQueryParser;
import com.trevari.book.dto.response.CacheableBookSearchResult;
import com.trevari.global.dto.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;

/**
 * 도서 캐시 전용 서비스 클래스
 * String 기반 Redis 캐싱으로 직렬화 문제 해결
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookCacheService {
    
    private final BookRepository bookRepository;
    private final SearchQueryParser searchQueryParser;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * 도서 검색 결과 캐시 처리 (String 기반 Redis 캐싱)
     * 
     * @param keyword  검색 키워드
     * @param pageable 페이징 정보
     * @return 캐시된 검색 결과
     */
    public CacheableBookSearchResult getCachedSearchResult(String keyword, Pageable pageable) {
        String cacheKey = "bookSearch:search:" + keyword + ":page:" + pageable.getPageNumber() + ":size:" + pageable.getPageSize();
        
        try {
            // 캐시에서 조회
            String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                log.debug("Cache HIT for keyword: {}", keyword);
                return objectMapper.readValue(cachedValue, CacheableBookSearchResult.class);
            }
            
            log.debug("Cache MISS for keyword: {}", keyword);
            
            // 검색 쿼리 파싱
            SearchQuery searchQuery = searchQueryParser.parse(keyword);
            log.debug("Parsed search query: {}", searchQuery);
            
            // 도서 검색 실행
            Page<Book> bookPage = bookRepository.searchBooks(searchQuery, pageable);
            
            // 응답 객체 생성 (execution time 제외)
            PageInfo pageInfo = PageInfo.of(bookPage);
            
            CacheableBookSearchResult result = CacheableBookSearchResult.from(
                keyword,
                pageInfo,
                bookPage.getContent(),
                searchQuery.strategy().name()
            );
            
            // 캐시에 저장 (TTL 5분)
            String jsonValue = objectMapper.writeValueAsString(result);
            stringRedisTemplate.opsForValue().set(cacheKey, jsonValue, Duration.ofMinutes(5));
            log.debug("Cached search result for keyword: {}", keyword);
            
            return result;
            
        } catch (JsonProcessingException e) {
            log.error("JSON processing error for keyword: {}", keyword, e);
            // 캐시 오류 시 DB에서 직접 조회
            return executeSearchWithoutCache(keyword, pageable);
        }
    }
    
    /**
     * 캐시 없이 검색 실행 (fallback)
     */
    private CacheableBookSearchResult executeSearchWithoutCache(String keyword, Pageable pageable) {
        SearchQuery searchQuery = searchQueryParser.parse(keyword);
        Page<Book> bookPage = bookRepository.searchBooks(searchQuery, pageable);
        PageInfo pageInfo = PageInfo.of(bookPage);
        
        return CacheableBookSearchResult.from(
            keyword,
            pageInfo,
            bookPage.getContent(),
            searchQuery.strategy().name()
        );
    }
    
    /**
     * 전체 도서 목록 캐시 처리 (String 기반 Redis 캐싱)
     * 
     * @param pageable 페이징 정보
     * @return 캐시된 전체 도서 목록
     */
    public CacheableBookSearchResult getAllBooksCached(Pageable pageable) {
        String cacheKey = "bookSearch:all:page:" + pageable.getPageNumber() + ":size:" + pageable.getPageSize();
        
        try {
            // 캐시에서 조회
            String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                log.debug("Cache HIT for all books");
                return objectMapper.readValue(cachedValue, CacheableBookSearchResult.class);
            }
            
            log.debug("Cache MISS for all books");
            
            // 전체 도서 조회
            Page<Book> bookPage = bookRepository.findAll(pageable);
            
            // 응답 객체 생성 (execution time 제외)
            PageInfo pageInfo = PageInfo.of(bookPage);
            
            CacheableBookSearchResult result = CacheableBookSearchResult.from(
                "", // 전체 조회이므로 빈 쿼리
                pageInfo,
                bookPage.getContent(),
                "ALL"
            );
            
            // 캐시에 저장 (TTL 10분 - 전체 목록은 자주 바뀌지 않으므로 길게)
            String jsonValue = objectMapper.writeValueAsString(result);
            stringRedisTemplate.opsForValue().set(cacheKey, jsonValue, Duration.ofMinutes(10));
            log.debug("Cached all books result");
            
            return result;
            
        } catch (JsonProcessingException e) {
            log.error("JSON processing error for all books", e);
            // 캐시 오류 시 DB에서 직접 조회
            return executeAllBooksSearchWithoutCache(pageable);
        }
    }
    
    /**
     * 캐시 없이 전체 도서 조회 (fallback)
     */
    private CacheableBookSearchResult executeAllBooksSearchWithoutCache(Pageable pageable) {
        Page<Book> bookPage = bookRepository.findAll(pageable);
        PageInfo pageInfo = PageInfo.of(bookPage);
        
        return CacheableBookSearchResult.from(
            "",
            pageInfo,
            bookPage.getContent(),
            "ALL"
        );
    }

    /**
     * 도서 상세 정보 캐시 처리 (String 기반 Redis 캐싱)
     * 
     * @param isbn 도서 ISBN
     * @return 캐시된 도서 정보
     */
    public Book getCachedBookByIsbn(String isbn) {
        String cacheKey = "bookDetail:" + isbn;
        
        try {
            // 캐시에서 조회
            String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                log.debug("Cache HIT for ISBN: {}", isbn);
                return objectMapper.readValue(cachedValue, Book.class);
            }
            
            log.debug("Cache MISS for ISBN: {}", isbn);
            
            // DB에서 조회
            Book book = bookRepository.findByIsbn(isbn).orElse(null);
            
            if (book != null) {
                // 캐시에 저장 (TTL 1시간)
                String jsonValue = objectMapper.writeValueAsString(book);
                stringRedisTemplate.opsForValue().set(cacheKey, jsonValue, Duration.ofHours(1));
                log.debug("Cached book for ISBN: {}", isbn);
            }
            
            return book;
            
        } catch (JsonProcessingException e) {
            log.error("JSON processing error for ISBN: {}", isbn, e);
            // 캐시 오류 시 DB에서 직접 조회
            return bookRepository.findByIsbn(isbn).orElse(null);
        }
    }

    /**
     * 카테고리별 도서 검색 결과 캐시 처리 (String 기반 Redis 캐싱)
     * 
     * @param categoryName 카테고리명
     * @param pageable 페이징 정보
     * @return 캐시된 검색 결과
     */
    public CacheableBookSearchResult getBooksByCategoryCached(String categoryName, Pageable pageable) {
        String cacheKey = "bookSearch:category:" + categoryName + ":page:" + pageable.getPageNumber() + ":size:" + pageable.getPageSize();
        
        try {
            // 캐시에서 조회
            String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                log.debug("Cache HIT for category: {}", categoryName);
                return objectMapper.readValue(cachedValue, CacheableBookSearchResult.class);
            }
            
            log.debug("Cache MISS for category: {}", categoryName);
            
            // 카테고리별 도서 검색
            Page<Book> bookPage = bookRepository.findByCategory(categoryName, pageable);
            
            // 응답 객체 생성 (execution time 제외)
            PageInfo pageInfo = PageInfo.of(bookPage);
            
            CacheableBookSearchResult result = CacheableBookSearchResult.from(
                "category:" + categoryName,
                pageInfo,
                bookPage.getContent(),
                "CATEGORY"
            );
            
            // 캐시에 저장 (TTL 10분)
            String jsonValue = objectMapper.writeValueAsString(result);
            stringRedisTemplate.opsForValue().set(cacheKey, jsonValue, Duration.ofMinutes(10));
            log.debug("Cached category search result for: {}", categoryName);
            
            return result;
            
        } catch (JsonProcessingException e) {
            log.error("JSON processing error for category: {}", categoryName, e);
            // 캐시 오류 시 DB에서 직접 조회
            return executeCategorySearchWithoutCache(categoryName, pageable);
        }
    }
    
    /**
     * 캐시 없이 카테고리별 도서 조회 (fallback)
     */
    private CacheableBookSearchResult executeCategorySearchWithoutCache(String categoryName, Pageable pageable) {
        Page<Book> bookPage = bookRepository.findByCategory(categoryName, pageable);
        PageInfo pageInfo = PageInfo.of(bookPage);
        
        return CacheableBookSearchResult.from(
            "category:" + categoryName,
            pageInfo,
            bookPage.getContent(),
            "CATEGORY"
        );
    }
}