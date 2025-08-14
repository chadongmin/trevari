package com.trevari.book.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trevari.book.domain.SearchKeyword;
import com.trevari.book.persistence.SearchKeywordJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
@DisplayName("SearchController 통합 테스트")
class SearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SearchKeywordJpaRepository searchKeywordRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        searchKeywordRepository.deleteAll();
        
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
    @DisplayName("인기 검색 키워드 API는 검색 횟수 순으로 정렬된 결과를 반환한다")
    void getPopularKeywords_ShouldReturnKeywordsSortedBySearchCount() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/search/popular")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Popular search keywords retrieved successfully"))
                .andExpect(jsonPath("$.data.keywords").isArray())
                .andExpect(jsonPath("$.data.keywords[0].keyword").value("java"))
                .andExpect(jsonPath("$.data.keywords[0].searchCount").value(100))
                .andExpect(jsonPath("$.data.keywords[1].keyword").value("spring"))
                .andExpect(jsonPath("$.data.keywords[1].searchCount").value(80))
                .andExpect(jsonPath("$.data.keywords[2].keyword").value("javascript"))
                .andExpect(jsonPath("$.data.keywords[2].searchCount").value(70))
                .andExpect(jsonPath("$.data.keywords[3].keyword").value("python"))
                .andExpect(jsonPath("$.data.keywords[3].searchCount").value(60))
                .andExpect(jsonPath("$.data.keywords[4].keyword").value("react"))
                .andExpect(jsonPath("$.data.keywords[4].searchCount").value(50));
    }

    @Test
    @DisplayName("인기 검색 키워드가 없을 때 빈 배열을 반환한다")
    void getPopularKeywords_WhenNoData_ShouldReturnEmptyArray() throws Exception {
        // Given
        searchKeywordRepository.deleteAll();

        // When & Then
        mockMvc.perform(get("/api/search/popular")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Popular search keywords retrieved successfully"))
                .andExpect(jsonPath("$.data.keywords").isArray())
                .andExpect(jsonPath("$.data.keywords").isEmpty());
    }

    @Test
    @DisplayName("인기 검색 키워드는 최대 10개까지만 반환한다")
    void getPopularKeywords_ShouldReturnMaximum10Keywords() throws Exception {
        // Given - 10개 이상의 키워드 생성
        searchKeywordRepository.deleteAll();
        for (int i = 1; i <= 15; i++) {
            SearchKeyword keyword = SearchKeyword.builder()
                    .keyword("keyword" + i)
                    .searchCount((long) (100 - i))
                    .build();
            searchKeywordRepository.saveSearchKeyword(keyword);
        }

        // When & Then
        mockMvc.perform(get("/api/search/popular")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.keywords").isArray())
                .andExpect(jsonPath("$.data.keywords.length()").value(10));
    }
}