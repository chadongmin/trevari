package com.trevari.book.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import com.trevari.book.IntegrationTestSupport;
import com.trevari.book.application.SearchKeywordService;
import com.trevari.book.domain.SearchKeyword;
import com.trevari.book.dto.PopularKeywordDto;
import com.trevari.book.persistence.SearchKeywordJpaRepository;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;


@DisplayName("MySQL vs Redis 성능 비교 테스트")
class RedisVsMySQLPerformanceTest extends IntegrationTestSupport {
    
    @Autowired
    private SearchKeywordService searchKeywordService;
    
    @Autowired
    private SearchKeywordJpaRepository searchKeywordRepository;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String TEST_KEYWORD = "performance_test";
    private static final String REDIS_KEY = "popular_keywords";
    
    @BeforeEach
    @Transactional
    void setUp() {
        // MySQL 테스트 데이터 초기화
        searchKeywordRepository.findByKeyword(TEST_KEYWORD)
            .ifPresent(keyword -> searchKeywordRepository.delete(keyword));
        
        // Redis 테스트 데이터 초기화 (Redis가 있는 경우만)
        if (redisTemplate != null) {
            redisTemplate.opsForZSet().remove(REDIS_KEY, TEST_KEYWORD);
        }
    }
    
    @Test
    @DisplayName("MySQL 방식 동시성 문제 재현")
    void testMySQLConcurrencyIssue() throws InterruptedException {
        final int THREAD_COUNT = 20;
        final int OPERATIONS_PER_THREAD = 5;
        final int EXPECTED_TOTAL = THREAD_COUNT * OPERATIONS_PER_THREAD;
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
        
        AtomicInteger successCount = new AtomicInteger(0);
        
        // MySQL 방식으로 동시성 테스트
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        searchKeywordService.recordSearchKeyword(TEST_KEYWORD);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        long mysqlTime = System.currentTimeMillis() - startTime;
        
        SearchKeyword result = searchKeywordRepository.findByKeyword(TEST_KEYWORD).orElse(null);
        
        System.out.println("=== MySQL 방식 동시성 테스트 결과 ===");
        System.out.println("예상 총 요청: " + EXPECTED_TOTAL);
        System.out.println("성공 요청: " + successCount.get());
        System.out.println("실제 저장된 카운트: " + (result != null ? result.getSearchCount() : 0));
        System.out.println("실행 시간: " + mysqlTime + "ms");
        
        if (result != null) {
            long actualCount = result.getSearchCount();
            double accuracy = (double) actualCount / EXPECTED_TOTAL * 100;
            System.out.println("데이터 정확도: " + String.format("%.2f%%", accuracy));
            System.out.println("데이터 손실률: " + String.format("%.2f%%", 100 - accuracy));
            
            // MySQL 방식은 동시성 문제로 인해 데이터 손실 발생 예상
            assertThat(actualCount).isLessThan(EXPECTED_TOTAL);
        }
    }
    
    @Test
    @DisplayName("Redis SortedSet 방식 동시성 해결 검증")
    void testRedisConcurrencySolution() throws InterruptedException {
        if (redisTemplate == null) {
            System.out.println("Redis not available, skipping Redis concurrency test");
            return;
        }
        
        final int THREAD_COUNT = 20;
        final int OPERATIONS_PER_THREAD = 5;
        final int EXPECTED_TOTAL = THREAD_COUNT * OPERATIONS_PER_THREAD;
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
        
        AtomicInteger successCount = new AtomicInteger(0);
        
        // Redis 방식으로 동시성 테스트
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        searchKeywordService.recordSearchKeywordWithRedis(TEST_KEYWORD);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        long redisTime = System.currentTimeMillis() - startTime;
        
        // Redis에서 결과 확인
        Double redisScore = redisTemplate.opsForZSet().score(REDIS_KEY, TEST_KEYWORD);
        long actualCount = redisScore != null ? redisScore.longValue() : 0L;
        
        System.out.println("=== Redis SortedSet 방식 동시성 테스트 결과 ===");
        System.out.println("예상 총 요청: " + EXPECTED_TOTAL);
        System.out.println("성공 요청: " + successCount.get());
        System.out.println("실제 저장된 카운트: " + actualCount);
        System.out.println("실행 시간: " + redisTime + "ms");
        
        double accuracy = (double) actualCount / EXPECTED_TOTAL * 100;
        System.out.println("데이터 정확도: " + String.format("%.2f%%", accuracy));
        
        // Redis 방식은 원자적 연산으로 100% 정확도 보장
        assertThat(actualCount).isEqualTo(EXPECTED_TOTAL);
        assertThat(accuracy).isEqualTo(100.0);
    }
    
    @Test
    @DisplayName("조회 성능 비교: MySQL vs Redis")
    void testQueryPerformanceComparison() {
        // 테스트 데이터 준비
        prepareTestData();
        
        // MySQL 조회 성능 측정
        long mysqlStartTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            List<SearchKeyword> mysqlResults = searchKeywordService.getTopSearchKeywords();
        }
        long mysqlTime = System.currentTimeMillis() - mysqlStartTime;
        
        // Redis 조회 성능 측정 (Redis가 있는 경우만)
        long redisTime = 0L;
        if (redisTemplate != null) {
            long redisStartTime = System.currentTimeMillis();
            for (int i = 0; i < 100; i++) {
                List<PopularKeywordDto> redisResults = searchKeywordService.getTopSearchKeywordsFromRedis();
            }
            redisTime = System.currentTimeMillis() - redisStartTime;
        }
        
        System.out.println("=== 조회 성능 비교 (100회 반복) ===");
        System.out.println("MySQL 총 시간: " + mysqlTime + "ms");
        System.out.println("MySQL 평균 시간: " + (mysqlTime / 100.0) + "ms");
        
        if (redisTime > 0) {
            System.out.println("Redis 총 시간: " + redisTime + "ms");
            System.out.println("Redis 평균 시간: " + (redisTime / 100.0) + "ms");
            System.out.println("성능 개선률: " + String.format("%.2fx", (double) mysqlTime / redisTime));
        } else {
            System.out.println("Redis not available - 성능 비교 불가");
        }
    }
    
    @Test
    @DisplayName("대용량 데이터에서의 성능 비교")
    void testLargeDataPerformance() {
        if (redisTemplate == null) {
            System.out.println("Redis not available, skipping large data performance test");
            return;
        }
        
        System.out.println("=== 대용량 데이터 성능 테스트 ===");
        
        // 1000개 키워드로 대용량 테스트 데이터 생성
        int keywordCount = 1000;
        
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < keywordCount; i++) {
            String keyword = "keyword_" + i;
            // 랜덤한 점수로 키워드 추가 (1-100 사이)
            int score = (int) (Math.random() * 100) + 1;
            redisTemplate.opsForZSet().incrementScore(REDIS_KEY, keyword, score);
        }
        long setupTime = System.currentTimeMillis() - startTime;
        
        // Redis Top 10 조회 성능 (대용량 데이터에서)
        startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            List<PopularKeywordDto> results = searchKeywordService.getTopSearchKeywordsFromRedis(10);
        }
        long redisQueryTime = System.currentTimeMillis() - startTime;
        
        System.out.println("테스트 데이터 생성: " + keywordCount + "개 키워드");
        System.out.println("데이터 생성 시간: " + setupTime + "ms");
        System.out.println("Redis 조회 시간 (100회): " + redisQueryTime + "ms");
        System.out.println("Redis 평균 조회 시간: " + (redisQueryTime / 100.0) + "ms");
        
        // 대용량 데이터에서도 일정한 성능 보장 검증
        assertThat(redisQueryTime / 100.0).isLessThan(10.0); // 평균 10ms 이하
    }
    
    @Test
    @DisplayName("실시간 랭킹 업데이트 시나리오")
    void testRealTimeRankingUpdate() {
        if (redisTemplate == null) {
            System.out.println("Redis not available, skipping real-time ranking test");
            return;
        }
        
        System.out.println("=== 실시간 랭킹 업데이트 테스트 ===");
        
        // 초기 키워드들 생성
        String[] keywords = {"java", "python", "javascript", "react", "spring"};
        for (String keyword : keywords) {
            redisTemplate.opsForZSet().incrementScore(REDIS_KEY, keyword, Math.random() * 50 + 1);
        }
        
        // 초기 랭킹 확인
        System.out.println("초기 랭킹:");
        List<PopularKeywordDto> initialRanking = searchKeywordService.getTopSearchKeywordsFromRedis(5);
        for (int i = 0; i < initialRanking.size(); i++) {
            PopularKeywordDto dto = initialRanking.get(i);
            System.out.println((i + 1) + ". " + dto.getKeyword() + " (" + dto.getCount() + ")");
        }
        
        // "java" 키워드를 대량 증가시켜 랭킹 변화 시뮬레이션
        for (int i = 0; i < 100; i++) {
            searchKeywordService.recordSearchKeywordWithRedis("java");
        }
        
        // 변경된 랭킹 확인
        System.out.println("\n'java' 100회 증가 후 랭킹:");
        List<PopularKeywordDto> updatedRanking = searchKeywordService.getTopSearchKeywordsFromRedis(5);
        for (int i = 0; i < updatedRanking.size(); i++) {
            PopularKeywordDto dto = updatedRanking.get(i);
            System.out.println((i + 1) + ". " + dto.getKeyword() + " (" + dto.getCount() + ")");
        }
        
        // "java"가 1위로 올라왔는지 확인
        assertThat(updatedRanking.get(0).getKeyword()).isEqualTo("java");
        assertThat(updatedRanking.get(0).getCount()).isGreaterThanOrEqualTo(100L);
    }
    
    private void prepareTestData() {
        // MySQL 테스트 데이터
        String[] testKeywords = {"java", "spring", "python", "javascript", "react"};
        for (int i = 0; i < testKeywords.length; i++) {
            SearchKeyword keyword = SearchKeyword.builder()
                .keyword(testKeywords[i])
                .searchCount((long) (50 - i * 10)) // 50, 40, 30, 20, 10
                .build();
            searchKeywordRepository.save(keyword);
        }
        
        // Redis 테스트 데이터 (Redis가 있는 경우만)
        if (redisTemplate != null) {
            for (int i = 0; i < testKeywords.length; i++) {
                redisTemplate.opsForZSet().incrementScore(REDIS_KEY, testKeywords[i], 50 - i * 10);
            }
        }
    }
}