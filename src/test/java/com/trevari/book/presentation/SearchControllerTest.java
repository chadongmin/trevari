package com.trevari.book.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trevari.book.application.BookService;
import com.trevari.book.application.SearchKeywordService;
import com.trevari.book.domain.SearchKeyword;
import com.trevari.book.dto.PopularKeywordDto;
import com.trevari.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchController 단위 테스트")
class SearchControllerTest {

    @Mock
    private BookService bookService;

    @Mock
    private SearchKeywordService searchKeywordService;

    @InjectMocks
    private SearchController searchController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(searchController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("인기 검색 키워드를 성공적으로 조회할 수 있다")
    void getPopularKeywords_ShouldReturnPopularKeywords() throws Exception {
        // Given - Redis 기반 DTO 사용
        List<PopularKeywordDto> mockKeywords = Arrays.asList(
                new PopularKeywordDto("java", 100L),
                new PopularKeywordDto("spring", 80L),
                new PopularKeywordDto("javascript", 70L)
        );
        given(searchKeywordService.getTopSearchKeywordsFromRedis()).willReturn(mockKeywords);

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
                .andExpect(jsonPath("$.data.keywords[2].searchCount").value(70));
    }

    @Test
    @DisplayName("검색 키워드가 없을 때 빈 배열을 반환한다")
    void getPopularKeywords_WhenNoKeywords_ShouldReturnEmptyArray() throws Exception {
        // Given - Redis 기반 메서드 사용
        given(searchKeywordService.getTopSearchKeywordsFromRedis()).willReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/search/popular")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Popular search keywords retrieved successfully"))
                .andExpect(jsonPath("$.data.keywords").isArray())
                .andExpect(jsonPath("$.data.keywords").isEmpty());
    }
}