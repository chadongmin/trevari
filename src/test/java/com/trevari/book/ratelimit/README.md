# Rate Limiting 테스트 가이드

## 테스트 구조

### 1. RateLimitUnitTest
- **목적**: Rate Limiting 로직의 단위 테스트
- **범위**: RateLimitAspect, RateLimitExceededException
- **의존성**: Mock 객체 사용
- **실행**: `./gradlew test --tests RateLimitUnitTest`

### 2. RateLimitServiceTest  
- **목적**: RateLimitService와 Redis 연동 테스트
- **범위**: Redis Lua 스크립트, Sliding Window 알고리즘
- **의존성**: 실제 Redis 서버 필요
- **실행**: `./gradlew test --tests RateLimitServiceTest -Dtest.redis.enabled=true`

### 3. RateLimitFunctionalTest
- **목적**: 전체 Rate Limiting 기능의 통합 테스트
- **범위**: API 레벨에서의 Rate Limiting 동작
- **의존성**: 실제 Redis 서버 필요
- **실행**: `./gradlew test --tests RateLimitFunctionalTest -Dtest.redis.enabled=true`

### 4. RateLimitIntegrationTest (기존)
- **목적**: 기본 통합 테스트
- **범위**: API 정상 동작 확인
- **의존성**: 테스트 프로파일 (Redis 불필요)

## 테스트 실행 방법

### 1. 전체 테스트 실행 (Redis 없음)
```bash
./gradlew test
```

### 2. Redis 연동 테스트 실행
```bash
# Redis 서버 시작
docker run -d -p 6379:6379 redis:latest

# Rate Limiting 테스트 실행
./gradlew test --tests "*RateLimit*" -Dtest.redis.enabled=true
```

### 3. 특정 테스트 클래스 실행
```bash
# 단위 테스트
./gradlew test --tests RateLimitUnitTest

# 서비스 테스트 (Redis 필요)
./gradlew test --tests RateLimitServiceTest -Dtest.redis.enabled=true

# 기능 테스트 (Redis 필요)
./gradlew test --tests RateLimitFunctionalTest -Dtest.redis.enabled=true
```

## 테스트 커버리지

### Unit Tests (RateLimitUnitTest)
- ✅ Rate Limit Aspect 정상 요청 처리
- ✅ Rate Limit 초과 시 예외 발생
- ✅ IP/GLOBAL 키 생성 전략
- ✅ X-Forwarded-For, X-Real-IP 헤더 처리
- ✅ 시간 단위 변환 (초, 분, 시간)
- ✅ 커스텀 키 SpEL 표현식 처리
- ✅ 예외 메시지 및 속성 검증

### Service Tests (RateLimitServiceTest)
- ✅ Redis Sliding Window 알고리즘
- ✅ 한도 내/초과 요청 처리
- ✅ Rate Limit 정보 조회
- ✅ 서로 다른 키의 독립적 처리
- ✅ 윈도우 만료 후 재요청 가능
- ✅ Fail-Open 정책 (Redis 장애 시)
- ✅ 대량 동시 요청 처리
- ✅ Redis 키 TTL 확인

### Functional Tests (RateLimitFunctionalTest)
- ✅ 도서 상세 조회 API (10초 3번 제한)
- ✅ 도서 검색 API (10초 3번 제한)
- ✅ 서로 다른 IP의 독립적 Rate Limiting
- ✅ 윈도우 만료 후 재요청 가능
- ✅ Rate Limit 예외 응답 형식 검증
- ✅ X-Real-IP 헤더 처리

## 면접관 테스트 시나리오

### 시나리오 1: 빠른 Rate Limit 확인
```bash
# 애플리케이션 실행
./gradlew bootRun

# 같은 IP에서 4번 연속 요청 (4번째에서 Rate Limit 발생)
curl -X GET http://localhost:8080/api/books/0201485397
curl -X GET http://localhost:8080/api/books/0201485397  
curl -X GET http://localhost:8080/api/books/0201485397
curl -X GET http://localhost:8080/api/books/0201485397  # 429 Error
```

### 시나리오 2: 자동화된 테스트 실행
```bash
# Redis 시작
docker run -d -p 6379:6379 redis:latest

# 전체 Rate Limiting 테스트 실행
./gradlew test --tests "*RateLimit*" -Dtest.redis.enabled=true

# 결과 확인
./gradlew test --tests RateLimitFunctionalTest -Dtest.redis.enabled=true --info
```

## 테스트 결과 예시

### 성공적인 테스트 출력
```
RateLimitFunctionalTest > testBookDetailRateLimit_3RequestsIn10Seconds() PASSED
RateLimitFunctionalTest > testBookSearchRateLimit_3RequestsIn10Seconds() PASSED
RateLimitFunctionalTest > testIndependentRateLimitForDifferentIPs() PASSED

RateLimitServiceTest > testTryAcquire_WithinLimit() PASSED
RateLimitServiceTest > testTryAcquire_ExceedsLimit() PASSED
RateLimitServiceTest > testSlidingWindowBehavior() PASSED

RateLimitUnitTest > testRateLimitAspect_SuccessfulRequest() PASSED
RateLimitUnitTest > testRateLimitAspect_RateLimitExceeded() PASSED
```

### Rate Limit 초과 시 응답 예시
```json
{
  "success": false,
  "code": 429,
  "message": "Rate limit exceeded. Limit: 3 requests per 10 seconds. Try again in 7 seconds.",
  "data": [],
  "timestamp": "2025-08-16T00:15:30"
}
```

## 주의사항

1. **Redis 의존성**: RateLimitServiceTest와 RateLimitFunctionalTest는 실제 Redis 서버가 필요합니다.
2. **시간 기반 테스트**: 일부 테스트는 실제 시간 대기가 필요할 수 있습니다.
3. **포트 충돌**: Redis 기본 포트(6379)가 사용 중이지 않은지 확인하세요.
4. **테스트 격리**: 각 테스트 전후로 Redis 키를 정리하여 테스트 간 영향을 방지합니다.

## 성능 벤치마크

Rate Limiting 시스템의 성능을 측정하려면:

```bash
# Apache Bench를 사용한 성능 테스트
ab -n 100 -c 10 http://localhost:8080/api/books/0201485397

# 또는 hey 사용
hey -n 100 -c 10 http://localhost:8080/api/books/0201485397
```

예상 결과: 처음 30개 요청은 성공, 나머지는 429 응답