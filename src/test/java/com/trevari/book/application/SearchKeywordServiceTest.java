package com.trevari.book.application;

import com.trevari.book.domain.SearchKeyword;
import com.trevari.book.domain.SearchKeywordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchKeywordService 단위 테스트")
class SearchKeywordServiceTest {

    @Mock
    private SearchKeywordRepository searchKeywordRepository;

    @InjectMocks
    private SearchKeywordService searchKeywordService;

    @BeforeEach
    void setUp() {
        // 각 테스트 전 초기화 로직 (필요시)
    }

    @Test
    @DisplayName("새로운 검색 키워드를 기록할 수 있다")
    void recordSearchKeyword_WhenNewKeyword_ShouldSaveNewKeyword() {
        // Given
        String keyword = "Java Programming";
        given(searchKeywordRepository.findByKeyword(anyString())).willReturn(Optional.empty());
        given(searchKeywordRepository.saveSearchKeyword(any(SearchKeyword.class))).willReturn(
                SearchKeyword.builder().keyword(keyword.toLowerCase()).build()
        );

        // When
        searchKeywordService.recordSearchKeyword(keyword);

        // Then
        then(searchKeywordRepository).should().findByKeyword("java programming");
        then(searchKeywordRepository).should().saveSearchKeyword(any(SearchKeyword.class));
    }

    @Test
    @DisplayName("기존 검색 키워드의 카운트를 증가시킬 수 있다")
    void recordSearchKeyword_WhenExistingKeyword_ShouldIncrementCount() {
        // Given
        String keyword = "Spring Boot";
        SearchKeyword existingKeyword = SearchKeyword.builder()
                .keyword("spring boot")
                .build();
        given(searchKeywordRepository.findByKeyword(anyString())).willReturn(Optional.of(existingKeyword));
        willDoNothing().given(searchKeywordRepository).incrementSearchCount(anyString());

        // When
        searchKeywordService.recordSearchKeyword(keyword);

        // Then
        then(searchKeywordRepository).should().findByKeyword("spring boot");
        then(searchKeywordRepository).should().incrementSearchCount("spring boot");
    }

    @Test
    @DisplayName("빈 문자열이나 null 키워드는 기록하지 않는다")
    void recordSearchKeyword_WhenEmptyOrNullKeyword_ShouldNotRecord() {
        // When & Then - null 케이스
        searchKeywordService.recordSearchKeyword(null);
        then(searchKeywordRepository).shouldHaveNoInteractions();

        // When & Then - 빈 문자열 케이스
        searchKeywordService.recordSearchKeyword("");
        then(searchKeywordRepository).shouldHaveNoInteractions();

        // When & Then - 공백만 있는 케이스
        searchKeywordService.recordSearchKeyword("   ");
        then(searchKeywordRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("상위 10개 인기 검색 키워드를 조회할 수 있다")
    void getTopSearchKeywords_ShouldReturnTop10Keywords() {
        // Given
        List<SearchKeyword> mockKeywords = Arrays.asList(
                SearchKeyword.builder().keyword("java").build(),
                SearchKeyword.builder().keyword("spring").build(),
                SearchKeyword.builder().keyword("javascript").build()
        );
        given(searchKeywordRepository.findTop10ByOrderBySearchCountDesc()).willReturn(mockKeywords);

        // When
        List<SearchKeyword> result = searchKeywordService.getTopSearchKeywords();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyElementsOf(mockKeywords);
        then(searchKeywordRepository).should().findTop10ByOrderBySearchCountDesc();
    }

    @Test
    @DisplayName("검색 키워드는 소문자로 정규화되어 저장된다")
    void recordSearchKeyword_ShouldNormalizeKeywordToLowercase() {
        // Given
        String keyword = "JAVA PROGRAMMING";
        given(searchKeywordRepository.findByKeyword(anyString())).willReturn(Optional.empty());
        given(searchKeywordRepository.saveSearchKeyword(any(SearchKeyword.class))).willReturn(
                SearchKeyword.builder().keyword("java programming").build()
        );

        // When
        searchKeywordService.recordSearchKeyword(keyword);

        // Then
        then(searchKeywordRepository).should().findByKeyword("java programming");
    }
}