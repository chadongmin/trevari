package com.trevari.book.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "출판 정보")
public class PublicationInfo {
    
    @ElementCollection
    @Schema(description = "저자 목록", example = "[\"Raoul-Gabriel Urma\", \"Mario Fusco\"]")
    private List<String> authors;
    
    @Schema(description = "출판사", example = "Manning Publications")
    private String publisher;
    
    @Schema(description = "출간일", example = "2020-01-01")
    private LocalDate publishedDate;
    
    /**
     * 대표 저자(첫 번째 저자) 반환
     *
     * @return 대표 저자명, 저자가 없으면 "Unknown Author"
     */
    public String getPrimaryAuthor() {
        if (authors == null || authors.isEmpty()) {
            return "Unknown Author";
        }
        return authors.get(0);
    }
    
    /**
     * 저자 목록을 문자열로 반환 (", "로 구분)
     *
     * @return 저자 목록 문자열
     */
    public String getAuthorsAsString() {
        if (authors == null || authors.isEmpty()) {
            return "Unknown Author";
        }
        return String.join(", ", authors);
    }
    
    /**
     * 공동 저자 존재 여부 확인
     *
     * @return 저자가 2명 이상이면 true
     */
    public boolean hasCoAuthors() {
        return authors != null && authors.size() > 1;
    }
    
    /**
     * 출간년도 반환
     *
     * @return 출간년도, 출간일이 없으면 0
     */
    public int getPublicationYear() {
        return publishedDate != null ? publishedDate.getYear() : 0;
    }
    
    /**
     * 출간 경과 연수 계산
     *
     * @return 현재 기준 출간 후 경과 연수
     */
    public int getYearsSincePublication() {
        if (publishedDate == null) {
            return 0;
        }
        return LocalDate.now().getYear() - publishedDate.getYear();
    }
    
    /**
     * 최근 출간 도서 여부 (5년 이내)
     *
     * @return 5년 이내 출간되었으면 true
     */
    public boolean isRecentPublication() {
        return getYearsSincePublication() <= 5;
    }
    
    /**
     * 출판사명 정규화 (앞뒤 공백 제거, 빈 문자열 처리)
     *
     * @return 정규화된 출판사명
     */
    public String getNormalizedPublisher() {
        if (publisher == null || publisher.trim().isEmpty()) {
            return "Unknown Publisher";
        }
        return publisher.trim();
    }
    
    /**
     * 저자 검색 (대소문자 구분 없음)
     *
     * @param searchName 검색할 저자명
     * @return 해당 저자가 포함되어 있으면 true
     */
    public boolean containsAuthor(String searchName) {
        if (authors == null || searchName == null) {
            return false;
        }
        return authors.stream()
            .anyMatch(author -> author.toLowerCase().contains(searchName.toLowerCase()));
    }
    
    /**
     * 저자 목록 불변 리스트 반환
     *
     * @return 저자 목록의 불변 복사본
     */
    public List<String> getAuthorsUnmodifiable() {
        if (authors == null) {
            return Collections.emptyList();
        }
        return List.copyOf(authors);
    }
    
    /**
     * 빈 저자 목록 정리
     *
     * @return 빈 문자열이나 null 저자를 제거한 새로운 PublicationInfo
     */
    public PublicationInfo cleanAuthors() {
        if (authors == null) {
            return this;
        }
        
        List<String> cleanedAuthors = authors.stream()
            .filter(author -> author != null && !author.trim().isEmpty())
            .map(String::trim)
            .toList();
        
        return PublicationInfo.builder()
            .authors(cleanedAuthors)
            .publisher(publisher)
            .publishedDate(publishedDate)
            .build();
    }
    
    /**
     * 출판 정보 유효성 검증
     *
     * @return 유효한 출판 정보이면 true
     */
    public boolean isValid() {
        return (authors != null && !authors.isEmpty()) &&
            (publisher != null && !publisher.trim().isEmpty()) &&
            (publishedDate != null && !publishedDate.isAfter(LocalDate.now()));
    }
}