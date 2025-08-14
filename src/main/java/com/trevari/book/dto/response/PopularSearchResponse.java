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
    
    public static PopularSearchResponse from(List<?> searchKeywords) {
        List<PopularKeyword> keywords = searchKeywords.stream()
                .map(obj -> {
                    // Handle Redis cache deserialization issue
                    if (obj instanceof SearchKeyword) {
                        SearchKeyword searchKeyword = (SearchKeyword) obj;
                        return PopularKeyword.from(searchKeyword);
                    } else if (obj instanceof java.util.Map<?, ?>) {
                        // Redis cache may return LinkedHashMap
                        java.util.Map<?, ?> map = (java.util.Map<?, ?>) obj;
                        String keyword = (String) map.get("keyword");
                        Long searchCount = ((Number) map.get("searchCount")).longValue();
                        return new PopularKeyword(keyword, searchCount);
                    } else {
                        throw new IllegalArgumentException("Unexpected object type: " + obj.getClass());
                    }
                })
                .toList();
        
        return new PopularSearchResponse(keywords);
    }
    
    // Overloaded method for type safety
    public static PopularSearchResponse fromSearchKeywords(List<SearchKeyword> searchKeywords) {
        List<PopularKeyword> keywords = searchKeywords.stream()
                .map(PopularKeyword::from)
                .toList();
        
        return new PopularSearchResponse(keywords);
    }
}