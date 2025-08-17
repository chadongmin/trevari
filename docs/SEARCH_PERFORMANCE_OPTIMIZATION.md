# 검색 성능 최적화 고민 과정

## 문제 상황

기존 도서 검색 시스템에서 성능 문제가 발생했습니다. 사용자가 검색할 때마다 느린 응답 시간으로 인해 사용자 경험이 나빠지고 있었습니다.

## 🔍 기존 검색 방식의 문제점

### 1. LIKE 검색의 한계

**기존 구현:**

```java
// CustomBookRepositoryImpl.java에서 사용하던 방식
BooleanExpression titleCondition = book.title.lower().contains(keyword.toLowerCase());
BooleanExpression subtitleCondition = book.subtitle.lower().contains(keyword.toLowerCase());
BooleanExpression authorCondition = book.bookAuthors.any().author.name.lower().contains(keyword.toLowerCase());

return titleCondition.

or(subtitleCondition).

or(authorCondition);
```

**문제점:**

- `%keyword%` 형태의 LIKE 검색은 인덱스를 제대로 활용하지 못함
- 특히 `LOWER()` 함수까지 사용하면 인덱스 스캔이 불가능
- 데이터가 많아질수록 Full Table Scan으로 인한 성능 저하
- 저자 검색 시 JOIN을 통한 추가적인 성능 오버헤드

### 2. 검색 결과의 관련성 문제

기존 방식으로는 검색어와 얼마나 관련이 있는지 판단할 수 없었습니다. 단순히 키워드가 포함된 모든 결과를 동일한 가중치로 반환했기 때문에 사용자가 원하는 결과를 찾기 어려웠습니다.

## 💡 해결 방안 고민

### 1. MySQL 풀텍스트 인덱스 도입 결정

**왜 풀텍스트 인덱스인가?**

풀텍스트 인덱스는 다음과 같은 장점이 있습니다:

- 키워드 검색에 특화된 인덱스 구조
- `MATCH() AGAINST()` 구문으로 관련성 점수 계산 가능
- 자연어 처리로 더 정확한 검색 결과 제공

**구현한 풀텍스트 검색:**

```sql
-- OptimizedBookRepository.java에서 사용
@Query(value = """
    (SELECT b.*, MATCH(b.title, b.subtitle) AGAINST(:keyword IN NATURAL LANGUAGE MODE) as relevance_score
     FROM book b 
     WHERE MATCH(b.title, b.subtitle) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
     ORDER BY relevance_score DESC)
    UNION
    (SELECT DISTINCT b.*, MATCH(a.name) AGAINST(:keyword IN NATURAL LANGUAGE MODE) as relevance_score
     FROM book b 
     JOIN book_author ba ON b.isbn = ba.book_isbn 
     JOIN author a ON ba.author_id = a.id
     WHERE MATCH(a.name) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
     ORDER BY relevance_score DESC)
    ORDER BY relevance_score DESC
    """, nativeQuery = true)
```

### 2. UNION 구조를 선택한 이유

**고민했던 다른 방식들:**

1. **LEFT JOIN 방식**: 모든 책과 저자를 조인하여 한 번에 검색
    - 문제: 중복 결과, 복잡한 GROUP BY 필요

2. **OR 조건 방식**: WHERE절에서 OR로 도서와 저자 조건 결합
    - 문제: 관련성 점수 계산이 복잡해짐

**UNION을 선택한 이유:**

- 도서 제목/부제목에서 찾은 결과와 저자명에서 찾은 결과를 명확히 분리
- 각각의 관련성 점수를 정확히 계산 가능
- 중복 제거가 자연스럽게 처리됨 (DISTINCT)
- 성능상 각각 최적화된 인덱스를 사용할 수 있음

### 3. Fallback 패턴 구현 이유

**왜 Fallback이 필요했나?**

풀텍스트 인덱스는 강력하지만 몇 가지 제약이 있습니다:

- 최소 키워드 길이 제한 (기본 4글자)
- 특정 문자나 기호 처리의 한계
- 인덱스가 없는 환경에서의 호환성 문제

**구현한 3단계 Fallback:**

```java
// CustomBookRepositoryImpl.java
public Page<Book> findByKeyword(String keyword, Pageable pageable) {
    // 1차: MySQL 풀텍스트 검색 시도
    if (optimizedBookRepository != null) {
        try {
            return optimizedBookRepository.findByFullTextSearch(keyword, pageable);
        } catch (Exception e) {
            log.warn("Full-text search failed, falling back to QueryDSL: {}", e.getMessage());
        }
    }

    // 2차: QueryDSL 최적화 검색
    // 3차: 기본 LIKE 검색 (마지막 수단)
}
```

이렇게 구현함으로써:

- 최적의 성능을 얻으면서도 100% 호환성 보장
- 인덱스 문제나 키워드 길이 제한 상황에서도 결과 제공
- 점진적 적용으로 시스템 안정성 확보

## 캐싱 추가

### Redis 캐싱을 도입한 이유

검색 성능이 개선되었지만, 여전히 인기 검색어는 반복적으로 같은 쿼리를 실행하게 됩니다. 특히 "Spring", "Java" 같은 키워드는 하루에도 수십 번 검색될 수 있습니다.

**캐싱 구현:**

```java
// BookCacheService.java
public CacheableBookSearchResult getCachedSearchResult(String keyword, Pageable pageable) {
    String cacheKey = "bookSearch:search:" + keyword + ":page:" +
            pageable.getPageNumber() + ":size:" + pageable.getPageSize();

    // 캐시 조회
    String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
    if (cachedValue != null) {
        return objectMapper.readValue(cachedValue, CacheableBookSearchResult.class);
    }

    // 캐시 미스 시 DB 검색 후 캐시 저장
    // TTL 5분으로 설정하여 메모리 효율성 확보
}
```

**5분 TTL을 선택한 이유:**

- 검색 결과의 실시간성과 캐시 효율성의 균형
- 도서 데이터는 자주 변경되지 않으므로 5분 정도는 적절
- 메모리 사용량을 제한하면서도 캐시 효과 극대화

## 실제 성능 개선 효과

### 캐시 적중률 기반 성능

- **캐시 HIT (70-80% 케이스)**: 1-3ms 응답
- **캐시 MISS + 풀텍스트**: 10-30ms 응답
- **캐시 MISS + QueryDSL Fallback**: 20-50ms 응답

### 트랜잭션 분리의 효과

**문제였던 상황:**
검색할 때마다 키워드를 기록하는데, 이 과정에서 트랜잭션 충돌이 발생했습니다.

**해결 방법:**

```java
// BookService.java에서 완전 분리
private void recordSearchKeywordAsync(String keyword) {
    try {
        searchKeywordService.recordSearchKeywordAsync(keyword);
    } catch (Exception e) {
        // 키워드 기록 실패해도 검색은 계속 진행
        log.warn("Failed to record keyword '{}': {}", keyword, e.getMessage());
    }
}
```

이를 통해 검색 성능에 영향을 주지 않으면서도 통계 수집을 할 수 있게 되었습니다.

## 결론

1. **풀텍스트 인덱스**로 근본적인 검색 성능 개선
2. **UNION 구조**로 정확한 관련성 계산
3. **Fallback 패턴**으로 호환성과 안정성 확보
4. **Redis 캐싱**으로 반복 검색 최적화
5. **트랜잭션 분리**로 부가 기능과 핵심 기능 독립
