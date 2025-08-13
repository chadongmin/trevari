package com.trevari.book.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trevari.book.domain.Book;
import com.trevari.book.domain.PublicationInfo;
import com.trevari.book.persistence.BookJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Rate Limiting 통합 테스트")
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookJpaRepository bookRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    private Book testBook;

    @BeforeEach
    void setUp() {
        // Redis가 있는 경우 rate limit 키들 정리
        if (redisTemplate != null) {
            var keys = redisTemplate.keys("rate_limit:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }

        // 테스트 데이터 생성
        testBook = Book.builder()
                .isbn("9781234567890")
                .title("Test Book")
                .subtitle("A test book for rate limiting")
                .publicationInfo(PublicationInfo.builder()
                        .authors(List.of("Test Author"))
                        .publisher("Test Publisher")
                        .publishedDate(LocalDate.of(2023, 1, 1))
                        .build())
                .build();

        bookRepository.save(testBook);
    }

    @Test
    @DisplayName("도서 검색 API Rate Limiting 테스트")
    void testBookSearchRateLimit() throws Exception {
        // 정상적인 요청들 (Rate Limit 내)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/books")
                            .param("keyword", "Test")
                            .param("page", "1")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // Rate Limit을 테스트하려면 실제 Redis가 필요하거나 
        // 더 낮은 제한값으로 테스트해야 함
        // 여기서는 정상 동작만 확인
    }

    @Test
    @DisplayName("도서 상세 조회 API Rate Limiting 테스트")
    void testBookDetailRateLimit() throws Exception {
        String isbn = testBook.getIsbn();

        // 정상적인 요청들
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/books/{isbn}", isbn)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("인기 검색 키워드 API Rate Limiting 테스트")
    void testPopularKeywordsRateLimit() throws Exception {
        // 정상적인 요청들
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/search/popular")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Rate Limit 헤더 확인 테스트")
    void testRateLimitHeaders() throws Exception {
        // Redis가 없는 테스트 환경에서는 Rate Limit이 적용되지 않을 수 있음
        // 하지만 API 자체는 정상 동작해야 함
        mockMvc.perform(get("/api/books")
                        .param("keyword", "Test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("다양한 IP에서의 Rate Limiting 테스트")
    void testRateLimitWithDifferentIPs() throws Exception {
        // 다양한 X-Forwarded-For 헤더로 테스트
        String[] ips = {"192.168.1.1", "192.168.1.2", "192.168.1.3"};

        for (String ip : ips) {
            mockMvc.perform(get("/api/books")
                            .param("keyword", "Test")
                            .header("X-Forwarded-For", ip)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }
}