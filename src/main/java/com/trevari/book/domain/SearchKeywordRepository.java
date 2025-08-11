package com.trevari.book.domain;

import java.util.List;
import java.util.Optional;

public interface SearchKeywordRepository {
    
    Optional<SearchKeyword> findByKeyword(String keyword);
    
    List<SearchKeyword> findTop10ByOrderBySearchCountDesc();
    
    SearchKeyword save(SearchKeyword searchKeyword);
    
    void incrementSearchCount(String keyword);
}