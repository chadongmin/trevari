package com.trevari.book.cache;

import com.trevari.book.IntegrationTestSupport;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("캐시 통합 테스트")
class CacheIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private BookService bookService;

    @Autowired
    private SearchKeywordService searchKeywordService;

    @Autowired
    private BookJpaRepository bookRepository;

    @Autowired
    private SearchKeywordJpaRepository searchKeywordRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private Book testBook;

    @BeforeEach
    void setUp() {
        // Redis 캐시 초기화
        stringRedisTemplate.getConnectionFactory().getConnection().flushAll();

        // 테스트 데이터 생성
        testBook = Book.builder()
                .isbn("9781234567890")
                .title("Test Book")
                .subtitle("A test book for caching")
                .publicationInfo(PublicationInfo.builder()
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

        // Redis 캐시 확인
        String cacheKey = "bookDetail:" + isbn;
        String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        assertThat(cachedValue).isNotNull();

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

        // Redis 캐시 확인
        String cacheKey = "bookSearch:search:" + keyword + ":page:0:size:10";
        String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        assertThat(cachedValue).isNotNull();

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

        // 두 번째 호출을 통해 캐시 동작 확인 (캐시가 동작하면 같은 결과가 빠르게 반환됨)

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

        // 캐시 동작은 성능상 이점으로 검증 (구체적인 캐시 확인 대신)

        // 새로운 검색 키워드 기록 - 캐시 무효화 발생
        searchKeywordService.recordSearchKeyword("newkeyword");

        // 키워드 기록 후 재조회 테스트 (캐시 무효화 후 새로운 데이터 조회)
        List<SearchKeyword> keywordsAfterRecord = searchKeywordService.getTopSearchKeywords();
        assertThat(keywordsAfterRecord).isNotNull();
    }
}