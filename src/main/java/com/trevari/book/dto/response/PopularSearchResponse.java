package com.trevari.book.dto.response;



import com.trevari.book.domain.SearchKeyword;

import java.util.List;

public record PopularSearchResponse(
        List<PopularKeyword> keywords
) {
    public record PopularKeyword(
            String keyword,
            long searchCount
    ) {
        public static PopularKeyword from(SearchKeyword searchKeyword) {
            return new PopularKeyword(
                    searchKeyword.getKeyword(),
                    searchKeyword.getSearchCount()
            );
        }
    }
    
    public static PopularSearchResponse from(List<SearchKeyword> searchKeywords) {
        List<PopularKeyword> keywords = searchKeywords.stream()
                .map(PopularKeyword::from)
                .toList();
        
        return new PopularSearchResponse(keywords);
    }
}