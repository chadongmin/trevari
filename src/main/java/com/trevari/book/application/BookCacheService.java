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
}