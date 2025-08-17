package com.trevari.book.concurrency;

import com.trevari.book.application.SearchKeywordService;
import com.trevari.book.domain.SearchKeyword;
import com.trevari.book.persistence.SearchKeywordJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@ActiveProfiles("test")
class SimpleConcurrencyTest {

    @Autowired
    private SearchKeywordService searchKeywordService;

    @Autowired
    private SearchKeywordJpaRepository searchKeywordRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        searchKeywordRepository.findByKeyword("testword")
                .ifPresent(keyword -> searchKeywordRepository.delete(keyword));
    }

    @Test
    void testConcurrentUpdates() throws InterruptedException {
        final int THREAD_COUNT = 10;
        final int OPERATIONS_PER_THREAD = 10;
        final int EXPECTED_TOTAL = THREAD_COUNT * OPERATIONS_PER_THREAD;

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        searchKeywordService.recordSearchKeyword("testword");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        SearchKeyword result = searchKeywordRepository.findByKeyword("testword").orElse(null);

        System.out.println("Expected: " + EXPECTED_TOTAL);
        System.out.println("Actual: " + (result != null ? result.getSearchCount() : 0));

        if (result != null) {
            long lost = EXPECTED_TOTAL - result.getSearchCount();
            if (lost > 0) {
                System.out.println("Data lost: " + lost + " (" + (lost * 100.0 / EXPECTED_TOTAL) + "%)");
            }
        }

        // 동시성 문제가 있다면 실제 카운트가 예상보다 작을 것
        if (result != null && result.getSearchCount() < EXPECTED_TOTAL) {
            System.out.println("Race condition detected!");
        }
    }
}