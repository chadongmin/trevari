# Rate Limiting Debug Report

## 개요
Docker Compose 환경에서 Rate Limiting이 정상 작동하지 않는 문제를 조사하고 해결하는 과정을 기록합니다.

## 문제 상황
- `docker-compose up`으로 실행 시 Rate Limiting이 작동하지 않음
- 브라우저에서 연속적인 API 요청 시에도 429 에러가 발생하지 않음
- Jackson 순환 참조 오류가 발생하여 Rate Limit Aspect가 실행되지 않을 가능성

## 해결 과정

### 1. Jackson 순환 참조 문제 해결 ✅
**문제**: BookAuthor ↔ Author 엔티티 간 순환 참조로 인한 Jackson 직렬화 오류
**해결방법**:
- `Book.java`: `@JsonManagedReference` 추가
- `BookAuthor.java`: `@JsonBackReference` 추가  
- `Author.java`: `@JsonIgnore` 추가

### 2. AOP 설정 확인 ✅
**확인사항**:
- `TrevariApplication.java`에 `@EnableAspectJAutoProxy` 추가됨
- `RateLimitAspect`와 `RateLimitService`에 `@Profile("!test")` 설정됨
- Docker 환경에서는 `docker` 프로파일 사용

### 3. Redis 연결 설정 확인 ✅
**Docker 환경 설정**:
- `application-docker.yml`에서 Redis 호스트를 `redis:6379`로 설정
- Redis 컨테이너 정상 실행 확인

### 4. Rate Limiting 테스트 진행중 🔄
**테스트 방법**:
1. 기존 API 엔드포인트 테스트
2. 새로운 테스트 컨트롤러 생성

## 현재 상태
- Jackson 순환 참조 문제 해결 완료
- Docker Compose 환경에서 애플리케이션 정상 실행
- Rate Limiting 작동 여부 확인 중

## 테스트 결과

### 새로운 테스트 엔드포인트 테스트
- `/api/test/rate-limit` 엔드포인트 생성
- 404 에러 발생 - 컨트롤러 스캔 문제 가능성

### Rate Limiting 작동 확인
**기존 엔드포인트 테스트**:
- `/api/search/books?keyword=Java` - Rate Limit: 3회/10초
- `/api/search/popular` - Rate Limit: 20회/1분

**테스트 결과**: 연속 요청에도 Rate Limiting이 작동하지 않음

### Rate Limit Aspect 로그 확인
**강화된 로깅 추가**:
- `System.out.println` 및 `log.error` 레벨로 로깅 추가
- `/api/search/books?keyword=TestAspect` 요청 수행

**결과**: Rate Limit Aspect가 전혀 실행되지 않음 
- 컨트롤러 로그는 정상 출력
- Aspect 로그는 전혀 나타나지 않음

## 발견된 문제
**Rate Limit Aspect가 실행되지 않는 이유**:
1. AOP 설정 문제 가능성
2. Bean 생성 실패 가능성  
3. @Profile 설정 문제 가능성

### Profile 설정 문제 발견 및 해결
**문제**: `@Profile("!test")` 설정으로 인한 Bean 생성 실패
**해결**: 
- `RateLimitAspect`에서 @Profile 제거
- `RateLimitService`에서 @Profile 제거  
- `RedisConfig`에서 @Profile 제거

### Bean 생성 로그 추가
**생성자 로깅 추가**:
- RateLimitAspect 생성자에 로깅
- RateLimitService 생성자에 로깅

**테스트 결과**: Bean 생성 로그가 여전히 나타나지 않음

## 최종 분석
**현재 상황**:
- Jackson 순환 참조 문제 해결됨
- @Profile 제한사항 제거됨  
- 애플리케이션 정상 시작
- **하지만 Rate Limit Bean들이 생성되지 않음**

## 최종 테스트 결과

### Rate Limiting 기능 테스트
**테스트 방법**: `/api/search/books` 엔드포인트에 4회 연속 요청 (Rate Limit: 3회/10초)
**기대 결과**: 4번째 요청에서 HTTP 429 응답
**실제 결과**: 모든 요청이 HTTP 200 응답

## 문제 해결 과정 요약

### ✅ 완료된 해결책
1. **Jackson 순환 참조 문제 해결**
   - BookAuthor ↔ Author 간 순환 참조 해결
   - 적절한 Jackson 어노테이션 적용

2. **@Profile 설정 문제 해결**
   - RateLimitAspect, RateLimitService, RedisConfig에서 @Profile("!test") 제거
   - Docker 환경에서 Bean 생성 가능하도록 설정

3. **AOP 설정 확인**
   - `@EnableAspectJAutoProxy` 메인 애플리케이션 클래스에 추가
   - Aspect 어노테이션 정상 설정

### 🔄 여전히 남은 문제
**Rate Limiting이 작동하지 않는 이유**:
- Bean 생성 로그가 나타나지 않음
- Aspect가 전혀 실행되지 않음
- 추가 디버깅 필요

### 권장 해결 방향
1. **Redis 연결 상태 직접 확인**
2. **Spring Boot Actuator를 통한 Bean 상태 확인**
3. **AOP 프록시 설정 재검토**
4. **의존성 문제 심화 분석**

## 결론
Jackson 순환 참조 문제는 완전히 해결되었으나, Rate Limiting 기능은 추가 분석이 필요한 상태입니다.