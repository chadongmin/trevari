package com.trevari.book.persistence;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.trevari.book.domain.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.trevari.book.domain.QBook.book;

/**
 * QueryDSL을 사용한 커스텀 Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class CustomBookRepositoryImpl implements CustomBookRepository {
    
    private final JPAQueryFactory queryFactory;
    
    @Override
    public Page<Book> findByKeyword(String keyword, Pageable pageable) {
        // 키워드 검색 조건 생성
        BooleanExpression searchCondition = createKeywordSearchCondition(keyword);
        
        // 데이터 조회
        List<Book> books = queryFactory
                .selectFrom(book)
                .leftJoin(book.bookAuthors).fetchJoin()
                .where(searchCondition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .distinct()
                .fetch();
        
        // 총 개수 조회
        Long totalCount = queryFactory
                .select(book.countDistinct())
                .from(book)
                .leftJoin(book.bookAuthors)
                .where(searchCondition)
                .fetchOne();
        
        return new PageImpl<>(books, pageable, totalCount != null ? totalCount : 0L);
    }
    
    @Override
    public Page<Book> findByOrKeywords(String keyword1, String keyword2, Pageable pageable) {
        // OR 연산 조건 생성
        BooleanExpression condition1 = createKeywordSearchCondition(keyword1);
        BooleanExpression condition2 = createKeywordSearchCondition(keyword2);
        
        BooleanExpression orCondition;
        if (condition1 != null && condition2 != null) {
            orCondition = condition1.or(condition2);
        } else if (condition1 != null) {
            orCondition = condition1;
        } else if (condition2 != null) {
            orCondition = condition2;
        } else {
            orCondition = null;
        }
        
        // 데이터 조회
        List<Book> books = queryFactory
                .selectFrom(book)
                .leftJoin(book.bookAuthors).fetchJoin()
                .where(orCondition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .distinct()
                .fetch();
        
        // 총 개수 조회
        Long totalCount = queryFactory
                .select(book.countDistinct())
                .from(book)
                .leftJoin(book.bookAuthors)
                .where(orCondition)
                .fetchOne();
        
        return new PageImpl<>(books, pageable, totalCount != null ? totalCount : 0L);
    }
    
    @Override
    public Page<Book> findByNotKeywords(String includeKeyword, String excludeKeyword, Pageable pageable) {
        // 포함 조건과 제외 조건 생성
        BooleanExpression includeCondition = createKeywordSearchCondition(includeKeyword);
        BooleanExpression excludeCondition = createKeywordSearchCondition(excludeKeyword);
        
        BooleanExpression notCondition;
        if (includeCondition != null && excludeCondition != null) {
            notCondition = includeCondition.and(excludeCondition.not());
        } else if (includeCondition != null) {
            notCondition = includeCondition; // exclude 키워드가 null이면 include만 적용
        } else {
            notCondition = null;
        }
        
        // 데이터 조회
        List<Book> books = queryFactory
                .selectFrom(book)
                .leftJoin(book.bookAuthors).fetchJoin()
                .where(notCondition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .distinct()
                .fetch();
        
        // 총 개수 조회
        Long totalCount = queryFactory
                .select(book.countDistinct())
                .from(book)
                .leftJoin(book.bookAuthors)
                .where(notCondition)
                .fetchOne();
        
        return new PageImpl<>(books, pageable, totalCount != null ? totalCount : 0L);
    }
    
    /**
     * 키워드 검색 조건을 생성하는 헬퍼 메서드
     * 제목, 부제목, 저자명을 대상으로 대소문자 무관 검색
     */
    private BooleanExpression createKeywordSearchCondition(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        
        String lowerKeyword = keyword.toLowerCase();
        
        // 제목, 부제목 조건
        BooleanExpression titleCondition = book.title.lower().contains(lowerKeyword);
        BooleanExpression subtitleCondition = book.subtitle.coalesce("").lower().contains(lowerKeyword);
        
        // 저자명 검색: BookAuthor와 Author 엔티티를 통한 검색
        BooleanExpression authorCondition = Expressions.booleanTemplate(
            "exists (select 1 from BookAuthor ba join ba.author a where ba.book = {0} and lower(a.name) like {1})",
            book,
            "%" + lowerKeyword + "%"
        );
        
        return titleCondition.or(subtitleCondition).or(authorCondition);
    }

    @Override
    public Page<Book> findByCategoryName(String categoryName, Pageable pageable) {
        // 카테고리 검색 조건 생성
        BooleanExpression categoryCondition = createCategorySearchCondition(categoryName);
        
        // 데이터 조회
        List<Book> books = queryFactory
                .selectFrom(book)
                .join(book.categories)
                .where(categoryCondition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .distinct()
                .fetch();
        
        // 총 개수 조회
        Long totalCount = queryFactory
                .select(book.countDistinct())
                .from(book)
                .join(book.categories)
                .where(categoryCondition)
                .fetchOne();
        
        return new PageImpl<>(books, pageable, totalCount != null ? totalCount : 0L);
    }
    
    /**
     * 카테고리 검색 조건을 생성하는 헬퍼 메서드
     */
    private BooleanExpression createCategorySearchCondition(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return null;
        }
        
        // QueryDSL을 사용한 카테고리 검색 - any() 메서드 사용
        return book.categories.any().name.containsIgnoreCase(categoryName);
    }
}
