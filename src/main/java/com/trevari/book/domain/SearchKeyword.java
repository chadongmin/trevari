package com.trevari.book.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_keywords")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchKeyword {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String keyword;
    
    @Column(nullable = false)
    private Long searchCount = 1L;
    
    // Timestamp fields temporarily commented out due to database schema mismatch
    // @Column(name = "created_at", nullable = false, updatable = false)
    // private LocalDateTime createdAt;
    
    // @Column(name = "updated_at", nullable = false)
    // private LocalDateTime updatedAt;
    
    @Builder
    public SearchKeyword(String keyword, Long searchCount) {
        this.keyword = keyword;
        this.searchCount = searchCount != null ? searchCount : 1L;
        // this.createdAt = LocalDateTime.now();
        // this.updatedAt = LocalDateTime.now();
    }
    
    public void incrementCount() {
        this.searchCount++;
        // this.updatedAt = LocalDateTime.now();
    }
    
    // @PrePersist
    // protected void onCreate() {
    //     this.createdAt = LocalDateTime.now();
    //     this.updatedAt = LocalDateTime.now();
    // }
    
    // @PreUpdate
    // protected void onUpdate() {
    //     this.updatedAt = LocalDateTime.now();
    // }
}