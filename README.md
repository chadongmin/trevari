# Trevari Book Search API

UI : http://34.58.7.194:8080/search

Swagger : http://34.58.7.194:8080/swagger-ui/index.html

## 주요 기능

아래와 같이 기본 검색 / OR 연산자 검색 / NOT 연산자 검색을 비롯한 검색 기능과
인기 검색어 조회, 도서 카테고리 별 검색 기능을 제공합니다.

```http
# 기본 검색
GET /api/books?keyword=Spring Boot&page=1&size=20

# OR 연산자 (Java 또는 Spring 포함)
GET /api/books?keyword=Java|Spring&page=1&size=20

# NOT 연산자 (Java 포함, Spring 제외)
GET /api/books?keyword=Java -Spring&page=1&size=20
```

### 성능 최적화

- **Redis 캐싱**: 자주 검색되는 결과를 메모리에 캐싱하여 응답 속도 **10-50배** 향상
- **인기 키워드 실시간 추적**: Redis ZSet을 활용한 O(log N) 성능의 키워드 랭킹
- **MySQL 풀텍스트 검색**: 자연어 처리 기반 정확한 검색 결과 제공

### ️ 시스템 보호

- **IP 기반 레이트 리미팅**: API별 차별화된 요청 제한으로 시스템 안정성 확보
- **Graceful Degradation**: 장애 상황에서도 서비스 지속성 보장

## 아키텍처

### 클린 아키텍처 구조

```
┌─ 🌐 Presentation Layer
│  ├─ Controllers (BookController, SearchController, CategoryController)
│  ├─ API Interfaces (BookApi, SearchApi, CategoryApi) 
│  └─ DTOs (Request/Response objects)
│
├─ 💼 Application Layer
│  ├─ Business Services (BookService, SearchKeywordService, CategoryService)
│  ├─ Cache Management (BookCacheService, SearchPerformanceService)
│  └─ Cross-cutting Concerns (RateLimitService, RateLimitAspect)
│
├─ 🎯 Domain Layer
│  ├─ Core Entities (Book, SearchKeyword, Category)
│  └─ Repository Interfaces (BookRepository, SearchKeywordRepository)
│
└─ 🔧 Infrastructure Layer
   ├─ JPA Implementations (BookJpaRepository, SearchKeywordJpaRepository)
   ├─ External Services (MySQL Database, Redis Cache)
   └─ Configuration (WebConfig, CacheConfig)
```

## 📖 도메인 모델

### Book Entity (도서)

```java

@Entity
public class Book {
    @Id
    private String isbn;           // ISBN (13자리, 고유 식별자)
    private String title;          // 도서 제목
    private String subtitle;       // 부제목 (선택사항)
    private String imageUrl;       // 도서 이미지 URL
    private String description;    // 도서 설명
    private Integer pageCount;     // 페이지 수

    @Enumerated(EnumType.STRING)
    private BookFormat format;     // 도서 형태 (HARDCOVER, PAPERBACK, EBOOK 등)

    @Embedded
    private PublicationInfo publicationInfo; // 출판 정보

    @Embedded
    private Price price;           // 가격 정보

    // Book ↔ Category: 다대다 관계
    @ManyToMany
    private Set<Category> categories;

    // Book ↔ Author: 다대다 관계 (BookAuthor 중간 테이블)
    @OneToMany(mappedBy = "book")
    private Set<BookAuthor> bookAuthors;
}
```

### Author Entity (저자)

```java

@Entity
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;           // 저자명

    // Author ↔ Book: 다대다 관계 (BookAuthor 중간 테이블)
    @OneToMany(mappedBy = "author")
    private Set<BookAuthor> bookAuthors;
}
```

### BookAuthor Entity (도서-저자 연결)

```java

@Entity
public class BookAuthor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Book book;             // 도서 참조

    @ManyToOne
    private Author author;         // 저자 참조

    private String role;           // 저자 역할 (저자, 공저자, 편저자 등)
}
```

### Category Entity (카테고리)

```java

@Entity
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;           // 카테고리명
}
```

### SearchKeyword Entity (검색 키워드)

```java

@Entity
public class SearchKeyword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String keyword;        // 검색 키워드
    private Integer count;         // 검색 횟수
    private LocalDateTime lastSearchedAt; // 마지막 검색 시간
}
```

### 임베디드 클래스들

#### PublicationInfo (출판 정보)

```java

@Embeddable
public class PublicationInfo {
    private String publisher;      // 출판사
    private LocalDate publishedDate; // 출간일

    // 출간년도, 최근 출간 여부 등 유틸리티 메서드 제공
    public int getPublicationYear() { ...}

    public boolean isRecentPublication() { ...}
}
```

#### Price (가격 정보)

```java

@Embeddable
public class Price {
    private Integer amount;        // 가격
    private String currency;       // 통화 (KRW, USD 등)
}
```

#### BookFormat (도서 형태)

```java
public enum BookFormat {
    BOOK,           // 일반 도서
    MAGAZINE,       // 잡지
    HARDCOVER,      // 양장본
    PAPERBACK,      // 문고본
    EBOOK           // 전자책
}
```

### 🔗 도메인 관계

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│    Book     │◄────────┤ BookAuthor  ├────────►│   Author    │
│   (도서)     │         │  (연결 테이블)  │         │   (저자)     │
└─────────────┘         └─────────────┘         └─────────────┘
       │
       │ M:N
       ▼
┌─────────────┐
│  Category   │
│  (카테고리)   │
└─────────────┘

┌─────────────┐
│SearchKeyword│
│ (검색 키워드)  │
└─────────────┘ (독립적 엔티티)
```

### 주요 관계 설명

- **Book ↔ Author**: 다대다 관계 (한 책은 여러 저자, 한 저자는 여러 책)
    - `BookAuthor` 중간 테이블을 통해 저자의 **역할(role)** 정보도 관리
- **Book ↔ Category**: 다대다 관계 (한 책은 여러 카테고리, 한 카테고리는 여러 책)
- **SearchKeyword**: 독립적 엔티티 (검색 통계 및 인기 키워드 관리)
- **임베디드 객체**: `PublicationInfo`, `Price`는 Book에 포함되는 값 객체

## 기술 스택

### Backend Core

- **Spring Boot 3.2** - 메인 프레임워크
- **Spring Data JPA** - ORM 및 데이터 접근 계층
- **QueryDSL** - 타입 안전한 동적 쿼리 생성
- **Spring Cache** - 캐싱 추상화 계층

### Database & Cache

- **MySQL 8.0** - 메인 데이터베이스 (도서 정보, 검색 기록)
- **Redis 6.2** - 캐시 스토어 (검색 결과, 인기 키워드, 레이트 리미팅)

### Test & Documentation

- **JUnit 5** - 단위 및 통합 테스트
- **JaCoCo** - 테스트 커버리지 측정 (현재 50% 달성)
- **OpenAPI 3.0 (Swagger)** - API 문서 자동 생성

## 실행 방법

### 사전 요구사항

- **Java 17** 이상
- **Docker & Docker Compose**
- **8080, 3306, 16379 포트** 사용 가능

### 1. 저장소 클론

```bash
git clone https://github.com/your-repo/trevari.git
cd trevari
```

### 2. Docker로 한 번에 실행

```bash
# 전체 시스템 시작 (MySQL + Redis + Application)
docker-compose up --build

# 백그라운드 실행
docker-compose up -d --build
```

### 4. 시드 데이터 생성

```bash
# 100개 이상의 샘플 도서 데이터 생성
curl -X POST http://localhost:8080/api/data/generate
```

## API 문서

### Swagger UI

개발 서버 실행 후 아래 링크에서 대화형 API 문서를 확인할 수 있습니다:

** [Swagger UI](http://localhost:8080/swagger-ui.html)**

### 주요 엔드포인트

#### 도서 검색 API

```http
GET /api/search/books?keyword={검색어}&page={페이지}&size={크기}
```

#### 도서 상세 조회 API

```http
GET /api/books/{isbn}
```

#### 인기 검색어 조회 API

```http
GET /api/search/popular
```

#### 카테고리 관리 API

```http
GET /api/categories
GET /api/categories/popular?limit={개수}
```

### 📝 응답 예시

```json
{
  "success": true,
  "data": {
    "searchQuery": "Spring Boot",
    "books": [
      ...
    ],
    "pageInfo": {
      "currentPage": 1,
      "pageSize": 20,
      "totalPages": 5,
      "totalElements": 95
    },
    "searchMetadata": {
      "executionTime": 23,
      "strategy": "SIMPLE",
      "totalElements": 95
    }
  },
  "message": "Books search completed successfully"
}
```

## 🛠️ 아키텍처 결정 사항

### 1. **클린 아키텍처 적용**

**결정**: Domain-Driven Design과 Clean Architecture 원칙 적용  
**이유**:

- 비즈니스 로직과 인프라의 명확한 분리
- 테스트 용이성 및 유지보수성 향상
- 향후 확장성과 기술 스택 변경 용이성

### 2. **Repository 패턴 구현**

**결정**: Domain Interface + JPA Repository 상속 방식  
**이유**:

- 도메인 계층의 순수성 유지
- JPA의 편의성과 도메인 설계의 장점 결합
- 테스트 시 Mock 객체 활용 용이

### 3. **API 인터페이스 분리**

**결정**: Controller와 API 스펙을 인터페이스로 분리  
**이유**:

- API 계약의 명확한 정의
- Swagger 문서 자동화
- 구현과 명세의 분리로 유지보수성 향상

## 문제 해결 중 고민 과정

### 1. 🔍 [검색 성능 최적화 고민](./docs/SEARCH_PERFORMANCE_OPTIMIZATION.md)

- **LIKE 검색 vs 풀텍스트 검색** 성능 트레이드오프 분석
- **UNION 구조 선택 이유**: 도서와 저자 검색 결과의 정확한 관련성 계산
- **Fallback 패턴 구현**: 풀텍스트 인덱스 제약사항 해결
- **Redis 캐싱 전략**: TTL 5분 선택 근거와 성능 효과
- **트랜잭션 분리**: 검색과 키워드 기록의 완전 독립

### 2. ⚡ [동시성 문제 해결 과정](./docs/MYSQL_REDIS_CONCURRENCY.md)

- **MySQL Race Condition** 발견과 분석 (90% 데이터 손실 문제)
- **Redis ZSet 활용**한 인기 검색어 TOP 10 구현
- **동시성 테스트**를 통한 검증과 개선 과정

### 3. 🛡️ [레이트 리미팅 구현 고민](./docs/RATE_LIMITING.md)

- **Sliding Window 알고리즘** 활용하여 구현
- **Redis Lua Script** 활용한 원자적 카운팅 구현