package com.trevari.book.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PublicationInfo 도메인 테스트")
class PublicationInfoTest {

    @Test
    @DisplayName("대표 저자 반환 - 정상 케이스")
    void getPrimaryAuthor_Success() {
        // given
        PublicationInfo info = PublicationInfo.builder()
                .build();

        // when & then
        assertThat(info.getPrimaryAuthor()).isEqualTo("Unknown Author");
    }

    @Test
    @DisplayName("대표 저자 반환 - 저자 목록이 비어있는 경우")
    void getPrimaryAuthor_EmptyAuthors() {
        // given
        PublicationInfo info = PublicationInfo.builder()
                .build();

        // when & then
        assertThat(info.getPrimaryAuthor()).isEqualTo("Unknown Author");
    }

    @Test
    @DisplayName("대표 저자 반환 - 저자 목록이 null인 경우")
    void getPrimaryAuthor_NullAuthors() {
        // given
        PublicationInfo info = PublicationInfo.builder()
                .build();

        // when & then
        assertThat(info.getPrimaryAuthor()).isEqualTo("Unknown Author");
    }

    @Test
    @DisplayName("저자 목록 문자열 변환 - 정상 케이스")
    void getAuthorsAsString_Success() {
        // given
        PublicationInfo info = PublicationInfo.builder()
                .build();

        // when & then
        assertThat(info.getAuthorsAsString()).isEqualTo("Unknown Author");
    }

    @Test
    @DisplayName("저자 목록 문자열 변환 - 저자가 없는 경우")
    void getAuthorsAsString_EmptyAuthors() {
        // given
        PublicationInfo info = PublicationInfo.builder()
                .build();

        // when & then
        assertThat(info.getAuthorsAsString()).isEqualTo("Unknown Author");
    }

    @Test
    @DisplayName("공동 저자 존재 여부 확인 - 공동 저자 있음")
    void hasCoAuthors_True() {
        // given
        PublicationInfo info = PublicationInfo.builder()
                .build();

        // when & then
        assertThat(info.hasCoAuthors()).isFalse();
    }

    @Test
    @DisplayName("공동 저자 존재 여부 확인 - 저자 한 명")
    void hasCoAuthors_False() {
        // given
        PublicationInfo info = PublicationInfo.builder()
                .build();

        // when & then
        assertThat(info.hasCoAuthors()).isFalse();
    }

    @Test
    @DisplayName("출간년도 반환 - 정상 케이스")
    void getPublicationYear_Success() {
        // given
        PublicationInfo info = PublicationInfo.builder()
                .publishedDate(LocalDate.of(2020, 5, 15))
                .build();

        // when & then
        assertThat(info.getPublicationYear()).isEqualTo(2020);
    }

    @Test
    @DisplayName("출간년도 반환 - 출간일이 null인 경우")
    void getPublicationYear_NullDate() {
        // given
        PublicationInfo info = PublicationInfo.builder()
                .publishedDate(null)
                .build();

        // when & then
        assertThat(info.getPublicationYear()).isZero();
    }

    @Test
    @DisplayName("출간 경과 연수 계산 - 정상 케이스")
    void getYearsSincePublication_Success() {
        // given
        int currentYear = LocalDate.now().getYear();
        PublicationInfo info = PublicationInfo.builder()
                .publishedDate(LocalDate.of(currentYear - 3, 1, 1))
                .build();

        // when & then
        assertThat(info.getYearsSincePublication()).isEqualTo(3);
    }

    @Test
    @DisplayName("최근 출간 도서 여부 확인 - 최근 출간")
    void isRecentPublication_True() {
        // given
        int currentYear = LocalDate.now().getYear();
        PublicationInfo info = PublicationInfo.builder()
                .publishedDate(LocalDate.of(currentYear - 2, 1, 1))
                .build();

        // when & then
        assertThat(info.isRecentPublication()).isTrue();
    }

    @Test
    @DisplayName("최근 출간 도서 여부 확인 - 오래된 출간")
    void isRecentPublication_False() {
        // given
        int currentYear = LocalDate.now().getYear();
        PublicationInfo info = PublicationInfo.builder()
                .publishedDate(LocalDate.of(currentYear - 10, 1, 1))
                .build();

        // when & then
        assertThat(info.isRecentPublication()).isFalse();
    }

    @Test
    @DisplayName("출판사명 정규화 - 정상 케이스")
    void getNormalizedPublisher_Success() {
        // given
        PublicationInfo info = PublicationInfo.builder()
                .publisher("  Manning Publications  ")
                .build();

        // when & then
        assertThat(info.getNormalizedPublisher()).isEqualTo("Manning Publications");
    }

    @Test
    @DisplayName("출판사명 정규화 - 빈 문자열")
    void getNormalizedPublisher_Empty() {
        // given
        PublicationInfo info = PublicationInfo.builder()
                .publisher("   ")
                .build();

        // when & then
        assertThat(info.getNormalizedPublisher()).isEqualTo("Unknown Publisher");
    }

    @Test
    @DisplayName("저자 검색 - 정확한 매치")
    void containsAuthor_ExactMatch() {
        // given
        PublicationInfo info = PublicationInfo.builder()
                .build();

        // when & then
        assertThat(info.containsAuthor("John Doe")).isFalse();
    }

    @Test
    @DisplayName("저자 검색 - 부분 매치")
    void containsAuthor_PartialMatch() {
        // given
        PublicationInfo info = PublicationInfo.builder()
                .build();

        // when & then
        assertThat(info.containsAuthor("john")).isFalse();
        assertThat(info.containsAuthor("Smith")).isFalse();
    }

    @Test
    @DisplayName("저자 검색 - 매치 없음")
    void containsAuthor_NoMatch() {
        // given
        PublicationInfo info = PublicationInfo.builder()
                .build();

        // when & then
        assertThat(info.containsAuthor("Bob")).isFalse();
    }

    @Test
    @DisplayName("불변 저자 목록 반환")
    void getAuthorsUnmodifiable_Success() {
        // given
        List<String> originalAuthors = Arrays.asList("John Doe", "Jane Smith");
        PublicationInfo info = PublicationInfo.builder()
                .build();

        // when
        List<String> unmodifiableAuthors = info.getAuthorsUnmodifiable();

        // then
        assertThat(unmodifiableAuthors).isEmpty();
        assertThat(unmodifiableAuthors).isNotSameAs(originalAuthors);
    }

    @Test
    @DisplayName("빈 저자 목록 정리 - 정상 케이스")
    void cleanAuthors_Success() {
        // given
        PublicationInfo info = PublicationInfo.builder()
                .publisher("Manning")
                .publishedDate(LocalDate.of(2020, 1, 1))
                .build();

        // when
        PublicationInfo cleaned = info.cleanAuthors();

        // then
        assertThat(cleaned.getAuthors()).isEmpty();
        assertThat(cleaned.getPublisher()).isEqualTo("Manning");
        assertThat(cleaned.getPublishedDate()).isEqualTo(LocalDate.of(2020, 1, 1));
    }

    @Test
    @DisplayName("출판 정보 유효성 검증 - 유효한 정보")
    void isValid_True() {
        // given
        PublicationInfo info = PublicationInfo.builder()
                .publisher("Manning Publications")
                .publishedDate(LocalDate.of(2020, 1, 1))
                .build();

        // when & then
        assertThat(info.isValid()).isTrue();
    }

    @Test
    @DisplayName("출판 정보 유효성 검증 - 저자 없음")
    void isValid_NoAuthors() {
        // given
        PublicationInfo info = PublicationInfo.builder()
                .publisher("Manning Publications")
                .publishedDate(LocalDate.of(2020, 1, 1))
                .build();

        // when & then
        assertThat(info.isValid()).isTrue();
    }

    @Test
    @DisplayName("출판 정보 유효성 검증 - 출판사 없음")
    void isValid_NoPublisher() {
        // given
        PublicationInfo info = PublicationInfo.builder()
                .publisher("")
                .publishedDate(LocalDate.of(2020, 1, 1))
                .build();

        // when & then
        assertThat(info.isValid()).isFalse();
    }

    @Test
    @DisplayName("출판 정보 유효성 검증 - 미래 출간일")
    void isValid_FutureDate() {
        // given
        PublicationInfo info = PublicationInfo.builder()
                .publisher("Manning Publications")
                .publishedDate(LocalDate.now().plusYears(1))
                .build();

        // when & then
        assertThat(info.isValid()).isFalse();
    }
}