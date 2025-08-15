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
    
    // 더 이상 사용하지 않음 - BookAuthor 엔티티로 대체됨
    // @ElementCollection
    // @Schema(description = "저자 목록", example = "[\"Raoul-Gabriel Urma\", \"Mario Fusco\"]")
    // private List<String> authors;
    
    @Schema(description = "출판사", example = "Manning Publications")
    private String publisher;
    
    @Schema(description = "출간일", example = "2020-01-01")
    private LocalDate publishedDate;
    
    /**
     * 대표 저자(첫 번째 저자) 반환 - 더 이상 사용되지 않음
     * BookAuthor 엔티티를 통해 조회해야 함
     *
     * @return "Unknown Author"
     */
    @Deprecated
    public String getPrimaryAuthor() {
        return "Unknown Author";
    }
    
    /**
     * 저자 목록 반환 - 더 이상 사용되지 않음
     * BookAuthor 엔티티를 통해 조회해야 함
     *
     * @return 빈 리스트
     */
    @Deprecated
    public List<String> getAuthors() {
        return Collections.emptyList();
    }
    
    /**
     * 저자 목록을 문자열로 반환 - 더 이상 사용되지 않음
     *
     * @return "Unknown Author"
     */
    @Deprecated
    public String getAuthorsAsString() {
        return "Unknown Author";
    }
    
    /**
     * 공동 저자 존재 여부 확인 - 더 이상 사용되지 않음
     *
     * @return false
     */
    @Deprecated
    public boolean hasCoAuthors() {
        return false;
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
     * 저자 검색 - 더 이상 사용되지 않음
     *
     * @param searchName 검색할 저자명
     * @return false
     */
    @Deprecated
    public boolean containsAuthor(String searchName) {
        return false;
    }
    
    /**
     * 저자 목록 불변 리스트 반환 - 더 이상 사용되지 않음
     *
     * @return 빈 리스트
     */
    @Deprecated
    public List<String> getAuthorsUnmodifiable() {
        return Collections.emptyList();
    }
    
    /**
     * 빈 저자 목록 정리 - 더 이상 사용되지 않음
     *
     * @return 현재 인스턴스
     */
    @Deprecated
    public PublicationInfo cleanAuthors() {
        return this;
    }
    
    /**
     * 출판 정보 유효성 검증 (저자 정보 제외)
     *
     * @return 유효한 출판 정보이면 true
     */
    public boolean isValid() {
        return (publisher != null && !publisher.trim().isEmpty()) &&
            (publishedDate != null && !publishedDate.isAfter(LocalDate.now()));
    }
}