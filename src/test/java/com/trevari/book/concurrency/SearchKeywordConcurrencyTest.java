package com.trevari.book.concurrency;

import com.trevari.book.application.SearchKeywordService;
import com.trevari.book.domain.SearchKeyword;
import com.trevari.book.persistence.SearchKeywordJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 검색 키워드 동시성 문제 검증 테스트
 * 현재 MySQL 기반 구현의 Race Condition을 재현하고 문제점을 입증
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("검색 키워드 동시성 테스트")
class SearchKeywordConcurrencyTest {

    @Autowired
    private SearchKeywordService searchKeywordService;

    @Autowired
    private SearchKeywordJpaRepository searchKeywordRepository;

    private static final String TEST_KEYWORD = "java";
    private static final int THREAD_COUNT = 50;
    private static final int REQUESTS_PER_THREAD = 20;
    private static final int EXPECTED_TOTAL_COUNT = THREAD_COUNT * REQUESTS_PER_THREAD;

    @BeforeEach
    @Transactional
    void setUp() {
        // 테스트 키워드 초기화
        searchKeywordRepository.findByKeyword(TEST_KEYWORD)
                .ifPresent(keyword -> searchKeywordRepository.delete(keyword));
    }

    @Test
    @DisplayName("동시성 환경에서 검색 키워드 카운트 Race Condition 검증")
    void testConcurrentKeywordCountingRaceCondition() throws InterruptedException {
        // Given: 멀티스레드 환경 설정
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        // When: 동시에 같은 키워드로 검색 기록
        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 동시에 시작하도록 대기
                    
                    for (int j = 0; j < REQUESTS_PER_THREAD; j++) {
                        try {
                            searchKeywordService.recordSearchKeyword(TEST_KEYWORD);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                            exceptions.add(e);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exceptions.add(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 모든 스레드 동시 시작
        startLatch.countDown();
        
        // 모든 작업 완료 대기 (최대 30초)
        boolean finished = endLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then: 결과 검증
        assertThat(finished).isTrue();
        
        // 키워드 조회 및 검증
        SearchKeyword savedKeyword = searchKeywordRepository.findByKeyword(TEST_KEYWORD)
                .orElse(null);

        System.out.println("=== 동시성 테스트 결과 ===");
        System.out.println("예상 총 요청 수: " + EXPECTED_TOTAL_COUNT);
        System.out.println("성공한 요청 수: " + successCount.get());
        System.out.println("실패한 요청 수: " + failureCount.get());
        
        if (savedKeyword != null) {
            System.out.println("실제 저장된 카운트: " + savedKeyword.getSearchCount());
            System.out.println("손실된 카운트: " + (EXPECTED_TOTAL_COUNT - savedKeyword.getSearchCount()));
            System.out.println("데이터 정확도: " + 
                String.format("%.2f%%", (double) savedKeyword.getSearchCount() / EXPECTED_TOTAL_COUNT * 100));
        } else {
            System.out.println("키워드가 저장되지 않음!");
        }
        
        if (!exceptions.isEmpty()) {
            System.out.println("발생한 예외 수: " + exceptions.size());
            System.out.println("예외 유형: " + exceptions.get(0).getClass().getSimpleName());
        }

        // 동시성 문제로 인한 데이터 손실 검증
        if (savedKeyword != null) {
            // Race Condition으로 인해 실제 카운트가 예상보다 적을 것으로 예상
            long actualCount = savedKeyword.getSearchCount();
            
            // 정확도 90% 미만이면 동시성 문제 존재로 판단
            double accuracy = (double) actualCount / EXPECTED_TOTAL_COUNT;
            
            if (accuracy < 0.9) {
                System.out.println("⚠️  동시성 문제 감지: 데이터 정확도 " + String.format("%.2f%%", accuracy * 100));
                System.out.println("📊 손실률: " + String.format("%.2f%%", (1 - accuracy) * 100));
            } else {
                System.out.println("✅ 동시성 문제 미감지 (정확도: " + String.format("%.2f%%", accuracy * 100) + ")");
            }
            
            // 예상 결과: MySQL의 Read-Modify-Write 패턴으로 인한 데이터 손실 발생
            assertThat(actualCount).isLessThan(EXPECTED_TOTAL_COUNT);
        }
    }

    @Test
    @DisplayName("순차 실행 시 정확성 검증 (베이스라인)")
    void testSequentialKeywordCountingBaseline() {
        // Given: 순차 실행 환경
        final int SEQUENTIAL_REQUESTS = 100;

        // When: 순차적으로 키워드 기록
        for (int i = 0; i < SEQUENTIAL_REQUESTS; i++) {
            searchKeywordService.recordSearchKeyword(TEST_KEYWORD);
        }

        // Then: 정확한 카운트 확인
        SearchKeyword savedKeyword = searchKeywordRepository.findByKeyword(TEST_KEYWORD)
                .orElse(null);

        assertThat(savedKeyword).isNotNull();
        assertThat(savedKeyword.getSearchCount()).isEqualTo(SEQUENTIAL_REQUESTS);
        
        System.out.println("=== 순차 실행 베이스라인 ===");
        System.out.println("예상 카운트: " + SEQUENTIAL_REQUESTS);
        System.out.println("실제 카운트: " + savedKeyword.getSearchCount());
        System.out.println("정확도: 100%");
    }

    @Test
    @DisplayName("동시성 문제로 인한 Lost Update 시나리오 재현")
    void testLostUpdateScenario() throws InterruptedException {
        // Given: 2개 스레드가 동시에 같은 키워드 업데이트
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);
        
        List<Long> finalCounts = new CopyOnWriteArrayList<>();

        // When: 동일한 타이밍에 업데이트 실행
        Runnable updateTask = () -> {
            try {
                startLatch.await();
                
                // 각 스레드에서 100번 업데이트
                for (int i = 0; i < 100; i++) {
                    searchKeywordService.recordSearchKeyword(TEST_KEYWORD);
                }
                
                // 최종 카운트 기록
                SearchKeyword keyword = searchKeywordRepository.findByKeyword(TEST_KEYWORD)
                        .orElse(null);
                if (keyword != null) {
                    finalCounts.add(keyword.getSearchCount());
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown();
            }
        };

        executorService.submit(updateTask);
        executorService.submit(updateTask);

        startLatch.countDown();
        endLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then: Lost Update 현상 확인
        SearchKeyword finalKeyword = searchKeywordRepository.findByKeyword(TEST_KEYWORD)
                .orElse(null);

        System.out.println("=== Lost Update 시나리오 ===");
        System.out.println("예상 최종 카운트: 200");
        if (finalKeyword != null) {
            System.out.println("실제 최종 카운트: " + finalKeyword.getSearchCount());
            
            // Lost Update로 인해 200보다 적을 가능성이 높음
            if (finalKeyword.getSearchCount() < 200) {
                System.out.println("⚠️  Lost Update 발생: " + (200 - finalKeyword.getSearchCount()) + "개 업데이트 손실");
            }
        }
        
        System.out.println("스레드별 관찰 카운트: " + finalCounts);
    }

    @Test
    @DisplayName("높은 동시성 환경에서의 성능 및 정확성 측정")
    void testHighConcurrencyPerformanceAndAccuracy() throws InterruptedException {
        // Given: 고부하 환경 시뮬레이션
        final int HIGH_THREAD_COUNT = 100;
        final int REQUESTS_PER_HIGH_THREAD = 10;
        final int EXPECTED_HIGH_TOTAL = HIGH_THREAD_COUNT * REQUESTS_PER_HIGH_THREAD;

        ExecutorService executorService = Executors.newFixedThreadPool(HIGH_THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(HIGH_THREAD_COUNT);
        
        long startTime = System.currentTimeMillis();

        // When: 높은 동시성으로 요청 실행
        for (int i = 0; i < HIGH_THREAD_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < REQUESTS_PER_HIGH_THREAD; j++) {
                        searchKeywordService.recordSearchKeyword(TEST_KEYWORD);
                    }
                } catch (Exception e) {
                    // 예외 발생 시에도 계속 진행
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(60, TimeUnit.SECONDS);
        executorService.shutdown();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Then: 성능 및 정확성 측정
        SearchKeyword savedKeyword = searchKeywordRepository.findByKeyword(TEST_KEYWORD)
                .orElse(null);

        System.out.println("=== 고부하 동시성 테스트 ===");
        System.out.println("총 실행 시간: " + totalTime + "ms");
        System.out.println("초당 처리량: " + String.format("%.2f", (double) EXPECTED_HIGH_TOTAL / totalTime * 1000) + " req/sec");
        
        if (savedKeyword != null) {
            double accuracy = (double) savedKeyword.getSearchCount() / EXPECTED_HIGH_TOTAL;
            System.out.println("예상 총 카운트: " + EXPECTED_HIGH_TOTAL);
            System.out.println("실제 저장 카운트: " + savedKeyword.getSearchCount());
            System.out.println("데이터 정확도: " + String.format("%.2f%%", accuracy * 100));
            System.out.println("데이터 손실률: " + String.format("%.2f%%", (1 - accuracy) * 100));
            
            // 고부하에서는 더 많은 데이터 손실 예상
            assertThat(accuracy).isLessThan(0.95);
        }
    }
}