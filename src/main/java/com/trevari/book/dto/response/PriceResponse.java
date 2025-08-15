package com.trevari.book.dto.response;

import com.trevari.book.domain.Price;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 가격 응답 DTO
 */
@Schema(description = "가격 정보")
public record PriceResponse(
    @Schema(description = "가격", example = "35000")
    Integer amount,
    
    @Schema(description = "통화", example = "KRW")
    String currency
) {
    
    public static PriceResponse from(Price price) {
        if (price == null) {
            return null;
        }
        return new PriceResponse(
            price.getAmount(),
            price.getCurrency()
        );
    }
}