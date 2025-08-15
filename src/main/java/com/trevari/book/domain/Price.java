package com.trevari.book.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 가격 정보를 위한 임베디드 클래스
 */
@Embeddable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Price {
    
    @Column(name = "amount")
    private Integer amount;
    
    @Column(name = "currency")
    private String currency;
}