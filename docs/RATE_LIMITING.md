# Rate Limiting 구현

## 개요

이 시스템은 **Sliding Window Counter** 알고리즘을 사용하여 API 호출 빈도를 제한하는 Rate Limiting 기능을 제공합니다. Redis를 백엔드 스토리지로 사용하여 분산 환경에서도 일관된
Rate Limiting을 보장합니다.

## 시스템 아키텍처

### 주요 컴포넌트

1. **@RateLimit 어노테이션**: 메서드 레벨에서 Rate Limiting 설정
2. **RateLimitAspect**: AOP를 통한 Rate Limiting 로직 적용
3. **RateLimitService**: Redis 기반 Rate Limiting 핵심 로직
4. **RateLimitExceededException**: Rate Limit 초과 시 발생하는 예외

### 시퀀스 다이어그램

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant RateLimitAspect
    participant RateLimitService
    participant Redis
    participant TargetMethod

    Client->>Controller: API 요청
    Controller->>RateLimitAspect: @RateLimit 어노테이션 감지
    
    RateLimitAspect->>RateLimitAspect: generateKey()
    Note over RateLimitAspect: IP/USER/GLOBAL 기반 키 생성<br/>예: BookController.getBookDetail(String):192.168.1.1
    
    RateLimitAspect->>RateLimitService: tryAcquire(key, limit, window)
    
    RateLimitService->>Redis: Lua Script 실행
    Note over Redis: Sliding Window Counter<br/>1. 만료된 요청 제거<br/>2. 현재 요청 수 확인<br/>3. 제한 내면 요청 추가
    
    alt Rate Limit 통과
        Redis-->>RateLimitService: {1, remaining_count}
        RateLimitService-->>RateLimitAspect: true
        RateLimitAspect->>TargetMethod: proceed()
        TargetMethod-->>RateLimitAspect: 결과 반환
        RateLimitAspect-->>Controller: 결과 반환
        Controller-->>Client: 성공 응답
    else Rate Limit 초과
        Redis-->>RateLimitService: {0, 0}
        RateLimitService->>RateLimitService: calculateRemainingTime()
        RateLimitService-->>RateLimitAspect: RateLimitExceededException
        RateLimitAspect-->>Controller: 예외 전파
        Controller-->>Client: 429 Too Many Requests
    end
```

## 알고리즘: Sliding Window Counter

### 동작 원리

1. **Redis Sorted Set 사용**: 각 요청을 timestamp를 score로 하여 저장
2. **만료된 요청 제거**: 현재 시간에서 윈도우 크기를 뺀 시간보다 이전 요청들 제거
3. **현재 요청 수 확인**: 윈도우 내 요청 수가 제한 이하인지 확인
4. **원자적 실행**: Lua 스크립트로 모든 연산을 원자적으로 처리

### Lua 스크립트 로직

```lua
local key = KEYS[1]
local window = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
local current_time = tonumber(ARGV[3])

-- 현재 윈도우의 시작 시간
local window_start = current_time - window

-- 만료된 요청들 제거
redis.call('ZREMRANGEBYSCORE', key, 0, window_start)

-- 현재 윈도우 내의 요청 수 조회
local current_requests = redis.call('ZCARD', key)

if current_requests < limit then
    -- 제한 내라면 현재 요청 추가
    redis.call('ZADD', key, current_time, current_time)
    redis.call('EXPIRE', key, window)
    return {1, limit - current_requests - 1}
else
    -- 제한 초과
    return {0, 0}
end
```

## 사용법

### 기본 사용법

```java

@RestController
public class BookController {

    @GetMapping("/{isbn}")
    @RateLimit(limit = 200, window = 1) // 1분당 200회
    public ResponseEntity<Book> getBookDetail(@PathVariable String isbn) {
        // 구현...
    }
}
```

### 고급 설정

```java
// 사용자별 제한 (인증 구현 시)
@RateLimit(limit = 1000, keyType = KeyType.USER)

// 전역 제한
@RateLimit(limit = 10000, keyType = KeyType.GLOBAL)

// 커스텀 키 (SpEL 표현식)
@RateLimit(limit = 100, key = "#isbn + ':' + #ip")

// 시간 단위 변경
@RateLimit(limit = 10, window = 1, timeUnit = TimeUnit.HOURS)
```

## 키 생성 전략

### 기본 키 형식

```
rate_limit:<method_signature>:<identifier>
```

### 키 타입별 예시

1. **IP 기반** (기본값)
   ```
   rate_limit:BookController.getBookDetail(String):192.168.1.1
   ```

2. **사용자 기반**
   ```
   rate_limit:BookController.getBookDetail(String):user123
   ```

3. **전역 기반**
   ```
   rate_limit:BookController.getBookDetail(String):global
   ```

4. **커스텀 키**
   ```
   rate_limit:BookController.getBookDetail(String):9780134685991:192.168.1.1
   ```

## 현재 적용된 Rate Limit 설정

| 엔드포인트                            | 제한          | 윈도우 | 비고          |
|----------------------------------|-------------|-----|-------------|
| `GET /api/books/{isbn}`          | 200 req/min | 1분  | 도서 상세 조회    |
| `GET /api/books/all`             | 100 req/min | 1분  | 전체 도서 조회    |
| `GET /api/books/category/{name}` | 100 req/min | 1분  | 카테고리별 도서 조회 |
| `GET /api/search/books`          | 100 req/min | 1분  | 도서 검색       |
| `GET /api/search/popular`        | 20 req/min  | 1분  | 인기 검색어 조회   |
| `GET /api/categories`            | 200 req/min | 1분  | 카테고리 목록     |
| `GET /api/categories/popular`    | 200 req/min | 1분  | 인기 카테고리     |

## 에러 처리

### Rate Limit 초과 시 응답

```json
{
  "success": false,
  "code": 429,
  "message": "Rate limit exceeded. Limit: 100 requests per 60 seconds. Try again in 45 seconds.",
  "data": [],
  "timestamp": "2025-08-16T00:15:30"
}
```

### HTTP 헤더

Rate Limit 정보는 HTTP 응답 헤더로도 제공됩니다:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1692151200
```

## 장점과 특징

### 장점

1. **정확성**: Sliding Window로 더 정확한 Rate Limiting
2. **분산 환경 지원**: Redis를 통한 일관된 상태 관리
3. **원자적 연산**: Lua 스크립트로 race condition 방지
4. **유연성**: 다양한 키 전략과 SpEL 표현식 지원
5. **장애 복구**: Redis 장애 시 fail-open 정책

### 특징

1. **메모리 효율성**: 만료된 요청 자동 정리
2. **실시간 모니터링**: 현재 사용량 조회 가능
3. **IP 추출**: 프록시 환경 고려한 실제 클라이언트 IP 추출
4. **테스트 친화적**: 테스트 환경에서 비활성화 가능

## 모니터링

### Redis 키 패턴

```bash
# 모든 Rate Limit 키 조회
redis-cli KEYS "rate_limit:*"

# 특정 키의 현재 요청 수 확인
redis-cli ZCARD "rate_limit:BookController.getBookDetail(String):192.168.1.1"

# 특정 키의 요청 내역 확인
redis-cli ZRANGE "rate_limit:BookController.getBookDetail(String):192.168.1.1" 0 -1 WITHSCORES
```

### 로그 모니터링

```bash
# Rate Limit 관련 로그 모니터링
tail -f application.log | grep "Rate limit"
```

## 설정 가능한 개선사항

1. **동적 설정**: 런타임에 Rate Limit 값 변경
2. **사용자별 차등 제한**: Premium 사용자에게 높은 한도 제공
3. **지역별 제한**: 지역에 따른 차등 Rate Limit
4. **API 키 기반 제한**: API 키별 개별 관리
5. **버스트 허용**: 짧은 시간 내 일정 수준의 버스트 허용

## 성능 고려사항

1. **Redis 연결 풀**: 적절한 연결 풀 크기 설정
2. **Lua 스크립트 캐싱**: 스크립트 SHA 캐싱으로 성능 향상
3. **TTL 설정**: 적절한 키 만료 시간 설정으로 메모리 사용량 관리
4. **배치 정리**: 주기적인 만료된 키 정리 작업