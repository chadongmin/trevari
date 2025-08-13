package com.trevari.book.application;

import com.trevari.book.domain.Book;
import com.trevari.book.domain.BookRepository;
import com.trevari.book.domain.search.SearchQuery;
import com.trevari.book.domain.search.SearchQueryParser;
import com.trevari.book.dto.response.BookSearchResponse;
import com.trevari.book.exception.BookException;
import com.trevari.book.exception.BookExceptionCode;
import com.trevari.global.dto.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
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
    
    private final BookRepository bookRepository;
    private final SearchQueryParser searchQueryParser;
    private final SearchKeywordService searchKeywordService;
    
    /**
     * ISBN으로 도서 단건 조회
     *
     * @param isbn 도서 ISBN
     * @return 조회된 도서 정보
     * @throws BookException 도서를 찾을 수 없는 경우
     */
    @Cacheable(value = "bookDetail", key = "#isbn")
    public Book getBookByIsbn(String isbn) {
        log.debug("Finding book by ISBN: {}", isbn);
        
        return bookRepository.findByIsbn(isbn)
            .orElseThrow(() -> {
                log.warn("Book not found with ISBN: {}", isbn);
                return new BookException(BookExceptionCode.BOOK_NOT_FOUND);
            });
    }
    
    /**
     * 키워드로 도서 검색
     *
     * @param keyword  검색 키워드
     * @param pageable 페이징 정보
     * @return 검색 결과
     */
    @Cacheable(value = "bookSearch", key = "#keyword + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public BookSearchResponse searchBooks(String keyword, Pageable pageable) {
        log.info("Searching books with keyword: {}, page: {}, size: {}",
            keyword, pageable.getPageNumber(), pageable.getPageSize());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 검색 쿼리 파싱
            SearchQuery searchQuery = searchQueryParser.parse(keyword);
            log.debug("Parsed search query: {}", searchQuery);
            
            // 도서 검색 실행
            Page<Book> bookPage = bookRepository.searchBooks(searchQuery, pageable);
            
            // 검색 키워드 기록 - Redis 방식 사용 (동시성 문제 해결)
            searchKeywordService.recordSearchKeywordWithRedis(keyword);
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Book search completed in {}ms, found {} books",
                executionTime, bookPage.getTotalElements());
            
            // 응답 객체 생성
            PageInfo pageInfo = PageInfo.of(bookPage);

//            return BookSearchResponse.builder()
//                    .searchQuery(keyword)
//                    .pageInfo(pageInfo)
//                    .books(bookPage.getContent())
//                    .searchMetadata(com.trevari.book.dto.response.SearchMetadata.of(
//                            executionTime,
//                            searchQuery.strategy().name()))
//                    .build();
            return BookSearchResponse.from(
                keyword,
                pageInfo,
                bookPage.getContent(),
                com.trevari.book.dto.response.SearchMetadata.of(
                    executionTime,
                    searchQuery.strategy().name())
            );
        } catch (IllegalArgumentException e) {
            log.warn("Invalid search query: {}", keyword, e);
            throw new BookException(BookExceptionCode.INVALID_SEARCH_KEYWORD);
        }
    }
}