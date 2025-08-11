package com.trevari.book.persistence;

import com.trevari.book.domain.SearchKeyword;
import com.trevari.book.domain.SearchKeywordRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchKeywordJpaRepository extends JpaRepository<SearchKeyword, Long>, SearchKeywordRepository {

    Optional<SearchKeyword> findByKeyword(String keyword);

    @Query("SELECT s FROM SearchKeyword s ORDER BY s.searchCount DESC LIMIT 10")
    List<SearchKeyword> findTop10ByOrderBySearchCountDesc();

    @Modifying
    @Query("UPDATE SearchKeyword s SET s.searchCount = s.searchCount + 1 WHERE s.keyword = :keyword")
    void incrementSearchCount(@Param("keyword") String keyword);
}