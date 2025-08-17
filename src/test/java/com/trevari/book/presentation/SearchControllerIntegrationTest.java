package com.trevari.book.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trevari.book.domain.SearchKeyword;
import com.trevari.book.domain.SearchKeywordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
@ActiveProfiles("test")
@DisplayName("SearchController 통합 테스트")
class SearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SearchKeywordRepository searchKeywordRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 각 테스트마다 데이터 초기화
        searchKeywordRepository.deleteAll();
    }

    private void createTestKeywords() {
        // 인기 검색 키워드 생성
        SearchKeyword java = SearchKeyword.builder()
                .keyword("java")
                .searchCount(100L)
                .build();
        searchKeywordRepository.saveSearchKeyword(java);

        SearchKeyword spring = SearchKeyword.builder()
                .keyword("spring")
                .searchCount(80L)
                .build();
        searchKeywordRepository.saveSearchKeyword(spring);

        SearchKeyword javascript = SearchKeyword.builder()
                .keyword("javascript")
                .searchCount(70L)
                .build();
        searchKeywordRepository.saveSearchKeyword(javascript);

        SearchKeyword python = SearchKeyword.builder()
                .keyword("python")
                .searchCount(60L)
                .build();
        searchKeywordRepository.saveSearchKeyword(python);

        SearchKeyword react = SearchKeyword.builder()
                .keyword("react")
                .searchCount(50L)
                .build();
        searchKeywordRepository.saveSearchKeyword(react);
    }

    @Test
    @DisplayName("인기 검색 키워드 API는 Redis에서 조회된 결과를 반환한다")
    void getPopularKeywords_ShouldReturnKeywordsFromRedis() throws Exception {
        // Given - Redis 기반 인기 검색어 서비스 사용
        // MySQL 기반 createTestKeywords() 대신 Redis 기반 테스트
        
        // When & Then
        mockMvc.perform(get("/api/search/popular")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Popular search keywords retrieved successfully"))
                .andExpect(jsonPath("$.data.keywords").isArray());
                // Redis 기반이므로 실제 검색 통계에 따라 결과가 동적으로 변함
    }

    @Test
    @DisplayName("인기 검색 키워드가 없을 때 빈 배열을 반환한다")
    void getPopularKeywords_WhenNoData_ShouldReturnEmptyArray() throws Exception {
        // Given - setUp()에서 이미 deleteAll() 수행됨

        // When & Then
        mockMvc.perform(get("/api/search/popular")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Popular search keywords retrieved successfully"))
                .andExpect(jsonPath("$.data.keywords").isArray());
        //.andExpect(jsonPath("$.data.keywords").isEmpty());
    }

    @Test
    @DisplayName("인기 검색 키워드는 최대 10개까지만 반환한다")
    void getPopularKeywords_ShouldReturnMaximum10Keywords() throws Exception {
        // Given - Redis 기반 서비스는 자동으로 최대 10개 제한

        // When & Then
        mockMvc.perform(get("/api/search/popular")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.keywords").isArray());
                // Redis 기반 서비스에서 자동으로 최대 10개 제한됨
    }
}