# 캐싱과 Rate Limiting 구현 가이드

## 📋 목차
1. [개요](#개요)
2. [캐싱 구현](#캐싱-구현)
3. [Rate Limiting 구현](#rate-limiting-구현)
4. [설정 및 환경별 구성](#설정-및-환경별-구성)
5. [테스트 구현](#테스트-구현)
6. [성능 최적화 고려사항](#성능-최적화-고려사항)
7. [향후 개선 방안](#향후-개선-방안)

## 개요

### 구현 목표
- **캐싱**: 자주 조회되는 데이터의 응답 속도 향상
- **Rate Limiting**: API 남용 방지 및 공정한 자원 분배
- **확장성**: Redis 기반 분산 환경 지원
- **안정성**: 장애 상황에서도 서비스 지속성 보장

### 기술 스택
- **캐싱**: Spring Cache + Redis
- **Rate Limiting**: Redis + Lua Script + Spring AOP
- **직렬화**: Jackson JSON
- **모니터링**: HTTP 헤더 기반 Rate Limit 정보 제공

## 캐싱 구현

### 1. 의존성 추가

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
}
```

### 2. Redis 캐시 설정

```java
@Configuration
@EnableCaching
@Profile("!test")
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Key serializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value serializer - JSON with type information
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // 기본 TTL 10분
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // 캐시별 개별 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("bookSearch", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("bookDetail", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("popularKeywords", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
```

### 3. 서비스 레이어 캐싱 적용

```java
@Service
@Transactional(readOnly = true)
public class BookService {
    
    // 도서 상세 정보 캐싱 (1시간)
    @Cacheable(value = "bookDetail", key = "#isbn")
    public Book getBookByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new BookException(BookExceptionCode.BOOK_NOT_FOUND));
    }
    
    // 도서 검색 결과 캐싱 (5분)
    @Cacheable(value = "bookSearch", key = "#keyword + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public BookSearchResponse searchBooks(String keyword, Pageable pageable) {
        // 검색 로직...
    }
}

@Service
public class SearchKeywordService {
    
    // 인기 키워드 캐싱 (30분)
    @Cacheable(value = "popularKeywords", key = "'top10'")
    public List<SearchKeyword> getTopSearchKeywords() {
        return searchKeywordRepository.findTop10ByOrderBySearchCountDesc();
    }
    
    // 키워드 기록 시 캐시 무효화
    @CacheEvict(value = "popularKeywords", allEntries = true)
    public void recordSearchKeyword(String keyword) {
        // 키워드 기록 로직...
    }
}
```

### 4. 캐시 키 전략

| 캐시명 | 키 전략 | TTL | 설명 |
|--------|---------|-----|------|
| `bookDetail` | `#{isbn}` | 1시간 | ISBN별 도서 상세 정보 |
| `bookSearch` | `#{keyword}:#{page}:#{size}` | 5분 | 검색 조건별 결과 |
| `popularKeywords` | `'top10'` | 30분 | 인기 키워드 목록 |

## Rate Limiting 구현

### 1. 커스텀 어노테이션 정의

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int limit() default 100;                    // 허용 요청 수
    long window() default 1;                    // 시간 윈도우
    TimeUnit timeUnit() default TimeUnit.MINUTES; // 시간 단위
    KeyType keyType() default KeyType.IP;       // 키 타입
    String key() default "";                    // 커스텀 키 (SpEL)
    
    enum KeyType {
        IP, USER, GLOBAL
    }
}
```

### 2. Redis 기반 Rate Limiting 서비스

```java
@Service
@Profile("!test")
public class RateLimitService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Sliding Window Counter 알고리즘 (Lua Script)
    private static final String RATE_LIMIT_LUA_SCRIPT = """
            local key = KEYS[1]
            local window = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])
            local current_time = tonumber(ARGV[3])
            
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
            """;
    
    public boolean tryAcquire(String key, int limit, long windowSeconds) {
        String redisKey = "rate_limit:" + key;
        long currentTime = System.currentTimeMillis() / 1000;
        
        try {
            List<Long> result = redisTemplate.execute(
                rateLimitScript,
                List.of(redisKey),
                windowSeconds, limit, currentTime
            );
            
            if (result != null && result.size() >= 2) {
                boolean allowed = result.get(0) == 1L;
                
                if (!allowed) {
                    long remainingTime = calculateRemainingTime(redisKey, windowSeconds);
                    throw new RateLimitExceededException(limit, windowSeconds, remainingTime);
                }
                
                return true;
            }
            
            return false;
        } catch (Exception e) {
            if (e instanceof RateLimitExceededException) {
                throw e;
            }
            // Redis 오류 시 요청 허용 (fail-open 정책)
            return true;
        }
    }
}
```

### 3. AOP 기반 Rate Limiting 적용

```java
@Aspect
@Component
@Profile("!test")
public class RateLimitAspect {
    
    private final RateLimitService rateLimitService;
    
    @Around("@annotation(rateLimit)")
    public Object handleRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = generateKey(joinPoint, rateLimit);
        long windowInSeconds = rateLimit.timeUnit().toSeconds(rateLimit.window());
        
        // Rate limit 체크
        rateLimitService.tryAcquire(key, rateLimit.limit(), windowInSeconds);
        
        // Rate limit 통과 시 원래 메서드 실행
        return joinPoint.proceed();
    }
    
    private String generateKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String baseKey = joinPoint.getSignature().toShortString();
        
        switch (rateLimit.keyType()) {
            case IP:
                return baseKey + ":" + getClientIP();
            case USER:
                return baseKey + ":" + getClientIP(); // 현재는 IP로 대체
            case GLOBAL:
                return baseKey + ":global";
            default:
                return baseKey + ":" + getClientIP();
        }
    }
    
    private String getClientIP() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        
        // X-Forwarded-For 헤더 확인
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        // X-Real-IP 헤더 확인
        String xRealIP = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIP)) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
}
```

### 4. Controller에 Rate Limiting 적용

```java
@RestController
@RequestMapping("/api/books")
public class BookController {
    
    @GetMapping("/{isbn}")
    @RateLimit(limit = 200, window = 1) // 200 requests per minute per IP
    public ResponseEntity<ApiResponse<Book>> getBookDetail(@PathVariable String isbn) {
        // 구현...
    }
    
    @GetMapping
    @RateLimit(limit = 100, window = 1) // 100 requests per minute per IP
    public ResponseEntity<ApiResponse<BookSearchResponse>> searchBooks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        // 구현...
    }
}

@RestController
@RequestMapping("/api/search")
public class SearchController {
    
    @GetMapping("/popular")
    @RateLimit(limit = 20, window = 1) // 20 requests per minute per IP  
    public ResponseEntity<ApiResponse<PopularSearchResponse>> getPopularKeywords() {
        // 구현...
    }
}
```

### 5. 예외 처리

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<List<ErrorDetail>>> handleRateLimitExceeded(
            RateLimitExceededException e, HttpServletRequest request) {
        
        ErrorDetail errorDetail = ErrorDetail.of("RATE_LIMIT_EXCEEDED", e.getMessage(), "rate_limit");
        
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("X-RateLimit-Limit", String.valueOf(e.getLimit()))
                .header("X-RateLimit-Window", String.valueOf(e.getWindowInSeconds()))
                .header("X-RateLimit-Retry-After", String.valueOf(e.getRemainingTime()))
                .body(new ApiResponse<>(
                        false,
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        "Rate limit exceeded",
                        List.of(errorDetail),
                        LocalDateTime.now()
                ));
    }
}
```

## 설정 및 환경별 구성

### 1. 운영 환경 설정 (application.yml)

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:
      timeout: 2000ms
      jedis:
        pool:
          max-active: 8
          max-wait: -1ms
          max-idle: 8
          min-idle: 0

  cache:
    type: redis
    redis:
      time-to-live: 600000 # 10분 (milliseconds)
```

### 2. 테스트 환경 설정 (application-test.yml)

```yaml
spring:
  cache:
    type: simple  # Redis 없이 메모리 캐시 사용
```

### 3. 테스트용 캐시 설정

```java
@Configuration
@Profile("test")
public class TestRedisConfig {

    @Bean
    @Primary
    public CacheManager testCacheManager() {
        return new ConcurrentMapCacheManager("bookSearch", "bookDetail", "popularKeywords");
    }
}
```

### 4. Docker Compose 설정

```yaml
# docker-compose.yml
services:
  redis:
    image: redis:6.2.6-alpine
    container_name: trevari-redis
    ports:
      - "16379:6379"
    networks:
      - network
```

## 테스트 구현

### 1. 캐싱 테스트

```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CacheIntegrationTest {

    @Autowired
    private BookService bookService;
    
    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("도서 상세 조회 캐싱 테스트")
    void testBookDetailCaching() {
        String isbn = testBook.getIsbn();

        // 첫 번째 호출 - 캐시 미스
        Book book1 = bookService.getBookByIsbn(isbn);
        
        // 캐시 확인
        var cache = cacheManager.getCache("bookDetail");
        assertThat(cache.get(isbn)).isNotNull();

        // 두 번째 호출 - 캐시 히트
        Book book2 = bookService.getBookByIsbn(isbn);
        assertThat(book2).isNotNull();
    }
}
```

### 2. Rate Limiting 테스트

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Rate Limit 헤더 확인 테스트")
    void testRateLimitHeaders() throws Exception {
        mockMvc.perform(get("/api/books")
                        .param("keyword", "Test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
```

## 성능 최적화 고려사항

### 1. Redis 연결 풀 최적화

```yaml
spring:
  data:
    redis:
      jedis:
        pool:
          max-active: 8     # 최대 활성 연결 수
          max-wait: -1ms    # 연결 대기 시간
          max-idle: 8       # 최대 유휴 연결 수
          min-idle: 0       # 최소 유휴 연결 수
```

### 2. 캐시 TTL 전략

- **짧은 TTL (5분)**: 자주 변경되는 검색 결과
- **중간 TTL (30분)**: 상대적으로 안정적인 인기 키워드
- **긴 TTL (1시간)**: 거의 변경되지 않는 도서 상세 정보

### 3. Lua 스크립트 사용 이유

- **원자성**: 여러 Redis 명령을 하나의 원자적 연산으로 실행
- **네트워크 효율성**: 여러 명령을 한 번의 라운드트립으로 처리
- **일관성**: Race condition 방지

### 4. Fail-Open 정책

```java
try {
    // Rate limiting 로직
} catch (Exception e) {
    if (e instanceof RateLimitExceededException) {
        throw e;
    }
    // Redis 오류 시 요청 허용
    return true;
}
```

## 향후 개선 방안

### 1. 캐싱 고도화

#### 캐시 워밍 (Cache Warming)
```java
@EventListener(ApplicationReadyEvent.class)
public void warmUpCache() {
    // 인기 검색어 미리 로드
    searchKeywordService.getTopSearchKeywords();
    
    // 인기 도서 미리 로드
    List<String> popularIsbns = getPopularBookIsbns();
    popularIsbns.forEach(bookService::getBookByIsbn);
}
```

#### 계층적 캐싱 (L1 + L2)
```java
@Cacheable(cacheNames = {"L1", "L2"}, key = "#isbn")
public Book getBookByIsbn(String isbn) {
    // L1: 로컬 메모리 캐시 (빠름, 작은 용량)
    // L2: Redis 캐시 (느림, 큰 용량)
}
```

#### 캐시 통계 및 모니터링
```java
@Component
public class CacheMetrics {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void handleCacheGetEvent(CacheGetEvent event) {
        Counter.builder("cache.gets")
                .tag("cache", event.getCacheName())
                .tag("result", event.getResult() != null ? "hit" : "miss")
                .register(meterRegistry)
                .increment();
    }
}
```

### 2. Rate Limiting 고도화

#### 동적 Rate Limit 조정
```java
@Component
public class DynamicRateLimitManager {
    
    // 서버 부하에 따른 동적 조정
    public int calculateDynamicLimit(String endpoint) {
        double cpuUsage = getCpuUsage();
        int baseLimit = getBaseLimit(endpoint);
        
        if (cpuUsage > 80) {
            return (int) (baseLimit * 0.5); // 부하 시 50% 감소
        } else if (cpuUsage < 30) {
            return (int) (baseLimit * 1.5); // 여유 시 50% 증가
        }
        
        return baseLimit;
    }
}
```

#### 사용자별 Rate Limiting
```java
@RateLimit(limit = 1000, keyType = KeyType.USER, key = "#authentication.name")
public ResponseEntity<?> searchBooks(Authentication authentication, ...) {
    // 인증된 사용자별 높은 한도 제공
}
```

#### Rate Limiting 우회 규칙
```java
@Component
public class RateLimitBypassRules {
    
    public boolean shouldBypass(String clientIP, String userAgent) {
        // 신뢰할 수 있는 IP 대역
        if (isTrustedIP(clientIP)) return true;
        
        // 내부 모니터링 도구
        if (isMonitoringAgent(userAgent)) return true;
        
        return false;
    }
}
```

### 3. 모니터링 및 알림

#### 캐시 성능 대시보드
```java
@Component
public class CacheHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // 캐시 히트율 확인
            double hitRate = getCacheHitRate();
            
            if (hitRate > 0.8) {
                return Health.up()
                        .withDetail("hit-rate", hitRate)
                        .build();
            } else {
                return Health.down()
                        .withDetail("hit-rate", hitRate)
                        .withDetail("reason", "Low cache hit rate")
                        .build();
            }
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

#### Rate Limiting 알림
```java
@EventListener
public void handleRateLimitExceeded(RateLimitExceededEvent event) {
    // 특정 IP가 지속적으로 Rate Limit에 걸리는 경우 알림
    if (isRepeatedOffender(event.getClientIP())) {
        alertingService.sendAlert(
            "Potential DDoS detected from IP: " + event.getClientIP()
        );
    }
}
```

### 4. 고급 캐싱 패턴

#### Write-Through 패턴
```java
@CachePut(value = "bookDetail", key = "#book.isbn")
public Book updateBook(Book book) {
    Book savedBook = bookRepository.save(book);
    // DB 업데이트와 동시에 캐시 갱신
    return savedBook;
}
```

#### Cache-Aside 패턴 with 분산 락
```java
@Retryable(value = Exception.class, maxAttempts = 3)
public Book getBookWithDistributedLock(String isbn) {
    String lockKey = "lock:book:" + isbn;
    
    try (RedisLock lock = redisLockManager.acquire(lockKey, Duration.ofSeconds(5))) {
        // 캐시 확인
        Book cached = getCachedBook(isbn);
        if (cached != null) return cached;
        
        // DB 조회 및 캐시 저장
        Book book = bookRepository.findByIsbn(isbn);
        cacheManager.getCache("bookDetail").put(isbn, book);
        return book;
    }
}
```

### 5. 성능 테스트 자동화

#### JMeter 기반 부하 테스트
```xml
<!-- rate-limit-test.jmx -->
<jmeterTestPlan>
  <hashTree>
    <TestPlan testname="Rate Limit Test">
      <elementProp>
        <Arguments>
          <Argument name="threads">100</Argument>
          <Argument name="ramp-up">10</Argument>
          <Argument name="duration">60</Argument>
        </Arguments>
      </elementProp>
    </TestPlan>
  </hashTree>
</jmeterTestPlan>
```

#### 캐시 성능 벤치마크
```java
@Component
public class CacheBenchmark {
    
    @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시
    public void runCacheBenchmark() {
        StopWatch stopWatch = new StopWatch();
        
        // 캐시 미스 성능 측정
        stopWatch.start();
        bookService.getBookByIsbn("uncached-isbn");
        stopWatch.stop();
        long cacheMissTime = stopWatch.getLastTaskTimeMillis();
        
        // 캐시 히트 성능 측정
        stopWatch.start();
        bookService.getBookByIsbn("cached-isbn");
        stopWatch.stop();
        long cacheHitTime = stopWatch.getLastTaskTimeMillis();
        
        // 성능 지표 기록
        meterRegistry.gauge("cache.performance.miss", cacheMissTime);
        meterRegistry.gauge("cache.performance.hit", cacheHitTime);
        meterRegistry.gauge("cache.performance.improvement", 
                           (double) cacheMissTime / cacheHitTime);
    }
}
```

이 가이드를 통해 캐싱과 Rate Limiting을 체계적으로 구현하고 지속적으로 개선할 수 있습니다. 각 섹션은 실제 운영 환경에서 검증된 모범 사례를 기반으로 작성되었으며, 성능과 안정성을 모두 고려한 설계를 제공합니다.