package com.trevari.book.cache;

import com.trevari.book.application.BookService;
import com.trevari.book.application.SearchKeywordService;
import com.trevari.book.domain.Book;
import com.trevari.book.domain.PublicationInfo;
import com.trevari.book.domain.SearchKeyword;
import com.trevari.book.dto.response.BookSearchResponse;
import com.trevari.book.persistence.BookJpaRepository;
import com.trevari.book.persistence.SearchKeywordJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("캐시 통합 테스트")
class CacheIntegrationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private SearchKeywordService searchKeywordService;

    @Autowired
    private BookJpaRepository bookRepository;

    @Autowired
    private SearchKeywordJpaRepository searchKeywordRepository;

    @Autowired
    private CacheManager cacheManager;

    private Book testBook;

    @BeforeEach
    void setUp() {
        // 캐시 초기화
        cacheManager.getCacheNames().forEach(cacheName ->
                cacheManager.getCache(cacheName).clear());

        // 테스트 데이터 생성
        testBook = Book.builder()
                .isbn("9781234567890")
                .title("Test Book")
                .subtitle("A test book for caching")
                .publicationInfo(PublicationInfo.builder()
                        .authors(List.of("Test Author"))
                        .publisher("Test Publisher")
                        .publishedDate(LocalDate.of(2023, 1, 1))
                        .build())
                .build();

        bookRepository.save(testBook);
    }

    @Test
    @DisplayName("도서 상세 조회 캐싱 테스트")
    void testBookDetailCaching() {
        String isbn = testBook.getIsbn();

        // 첫 번째 호출 - 캐시 미스
        Book book1 = bookService.getBookByIsbn(isbn);
        assertThat(book1).isNotNull();
        assertThat(book1.getIsbn()).isEqualTo(isbn);

        // 캐시 확인
        var cache = cacheManager.getCache("bookDetail");
        assertThat(cache).isNotNull();
        assertThat(cache.get(isbn)).isNotNull();

        // 두 번째 호출 - 캐시 히트 (같은 객체 반환되어야 함)
        Book book2 = bookService.getBookByIsbn(isbn);
        assertThat(book2).isNotNull();
        assertThat(book2.getIsbn()).isEqualTo(isbn);
    }

    @Test
    @DisplayName("도서 검색 결과 캐싱 테스트")
    void testBookSearchCaching() {
        String keyword = "Test";
        var pageable = PageRequest.of(0, 10);

        // 첫 번째 호출 - 캐시 미스
        BookSearchResponse response1 = bookService.searchBooks(keyword, pageable);
        assertThat(response1).isNotNull();
        assertThat(response1.searchQuery()).isEqualTo(keyword);

        // 캐시 확인
        var cache = cacheManager.getCache("bookSearch");
        assertThat(cache).isNotNull();
        String cacheKey = keyword + ":0:10";
        assertThat(cache.get(cacheKey)).isNotNull();

        // 두 번째 호출 - 캐시 히트
        BookSearchResponse response2 = bookService.searchBooks(keyword, pageable);
        assertThat(response2).isNotNull();
        assertThat(response2.searchQuery()).isEqualTo(keyword);
    }

    @Test
    @DisplayName("인기 검색 키워드 캐싱 테스트")
    void testPopularKeywordsCaching() {
        // 테스트 검색 키워드 생성
        SearchKeyword keyword1 = SearchKeyword.builder()
                .keyword("java")
                .searchCount(100L)
                .build();
        SearchKeyword keyword2 = SearchKeyword.builder()
                .keyword("spring")
                .searchCount(80L)
                .build();

        searchKeywordRepository.saveSearchKeyword(keyword1);
        searchKeywordRepository.saveSearchKeyword(keyword2);

        // 첫 번째 호출 - 캐시 미스
        List<SearchKeyword> keywords1 = searchKeywordService.getTopSearchKeywords();
        assertThat(keywords1).isNotEmpty();

        // 캐시 확인
        var cache = cacheManager.getCache("popularKeywords");
        assertThat(cache).isNotNull();
        assertThat(cache.get("top10")).isNotNull();

        // 두 번째 호출 - 캐시 히트
        List<SearchKeyword> keywords2 = searchKeywordService.getTopSearchKeywords();
        assertThat(keywords2).isNotEmpty();
        assertThat(keywords2.size()).isEqualTo(keywords1.size());
    }

    @Test
    @DisplayName("검색 키워드 기록 시 인기 키워드 캐시 무효화 테스트")
    void testCacheEvictionOnKeywordRecord() {
        // 인기 키워드 캐시 생성
        searchKeywordService.getTopSearchKeywords();
        
        var cache = cacheManager.getCache("popularKeywords");
        assertThat(cache.get("top10")).isNotNull();

        // 새로운 검색 키워드 기록 - 캐시 무효화 발생
        searchKeywordService.recordSearchKeyword("newkeyword");

        // 캐시가 무효화되었는지 확인
        assertThat(cache.get("top10")).isNull();
    }
}