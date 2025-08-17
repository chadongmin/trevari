package com.trevari.book.ratelimit;

import com.trevari.global.ratelimit.RateLimit;
import com.trevari.global.ratelimit.RateLimitAspect;
import com.trevari.global.ratelimit.RateLimitExceededException;
import com.trevari.global.ratelimit.RateLimitService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Rate Limiting 단위 테스트
 * Aspect와 Service의 로직을 개별적으로 테스트합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Rate Limiting 단위 테스트")
class RateLimitServiceTest {

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    @InjectMocks
    private RateLimitAspect rateLimitAspect;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Use lenient stubbing for setup mocks that may not be used in all tests
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.toShortString()).thenReturn("BookController.getBookDetail(String)");
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{"9781234567890"});
    }

    @Test
    @DisplayName("Rate Limit Aspect - 정상 요청 처리")
    void testRateLimitAspect_SuccessfulRequest() throws Throwable {
        // Given
        RateLimit rateLimit = createRateLimit(3, 10, TimeUnit.SECONDS);
        Object expectedResult = "success";

        when(rateLimitService.tryAcquire(anyString(), eq(3), eq(10L))).thenReturn(true);
        when(joinPoint.proceed()).thenReturn(expectedResult);

        // When
        Object result = rateLimitAspect.handleRateLimit(joinPoint, rateLimit);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        verify(rateLimitService).tryAcquire(
                eq("BookController.getBookDetail(String):192.168.1.1"),
                eq(3),
                eq(10L)
        );
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("Rate Limit Aspect - Rate Limit 초과 시 예외 발생")
    void testRateLimitAspect_RateLimitExceeded() throws Throwable {
        // Given
        RateLimit rateLimit = createRateLimit(3, 10, TimeUnit.SECONDS);

        doThrow(new RateLimitExceededException(3, 10, 5))
                .when(rateLimitService).tryAcquire(anyString(), eq(3), eq(10L));

        // When & Then
        assertThatThrownBy(() -> rateLimitAspect.handleRateLimit(joinPoint, rateLimit))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Rate limit exceeded")
                .hasMessageContaining("3 requests per 10 seconds")
                .hasMessageContaining("Try again in 5 seconds");

        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("Rate Limit 키 생성 - IP 기반")
    void testKeyGeneration_IPBased() throws Throwable {
        // Given
        RateLimit rateLimit = createRateLimit(3, 10, TimeUnit.SECONDS, RateLimit.KeyType.IP);

        when(rateLimitService.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        // When
        rateLimitAspect.handleRateLimit(joinPoint, rateLimit);

        // Then
        verify(rateLimitService).tryAcquire(
                eq("BookController.getBookDetail(String):192.168.1.1"),
                eq(3),
                eq(10L)
        );
    }

    @Test
    @DisplayName("Rate Limit 키 생성 - GLOBAL 기반")
    void testKeyGeneration_GlobalBased() throws Throwable {
        // Given
        RateLimit rateLimit = createRateLimit(3, 10, TimeUnit.SECONDS, RateLimit.KeyType.GLOBAL);

        when(rateLimitService.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        // When
        rateLimitAspect.handleRateLimit(joinPoint, rateLimit);

        // Then
        verify(rateLimitService).tryAcquire(
                eq("BookController.getBookDetail(String):global"),
                eq(3),
                eq(10L)
        );
    }

    @Test
    @DisplayName("IP 추출 - X-Forwarded-For 헤더 우선")
    void testIPExtraction_XForwardedForHeader() throws Throwable {
        // Given
        request.addHeader("X-Forwarded-For", "203.0.113.1, 192.168.1.1");
        request.addHeader("X-Real-IP", "198.51.100.1");
        request.setRemoteAddr("192.168.1.100");

        RateLimit rateLimit = createRateLimit(3, 10, TimeUnit.SECONDS);

        when(rateLimitService.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        // When
        rateLimitAspect.handleRateLimit(joinPoint, rateLimit);

        // Then
        verify(rateLimitService).tryAcquire(
                eq("BookController.getBookDetail(String):203.0.113.1"), // 첫 번째 IP 사용
                eq(3),
                eq(10L)
        );
    }

    @Test
    @DisplayName("IP 추출 - X-Real-IP 헤더 사용")
    void testIPExtraction_XRealIPHeader() throws Throwable {
        // Given
        request.addHeader("X-Real-IP", "198.51.100.1");
        request.setRemoteAddr("192.168.1.100");

        RateLimit rateLimit = createRateLimit(3, 10, TimeUnit.SECONDS);

        when(rateLimitService.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        // When
        rateLimitAspect.handleRateLimit(joinPoint, rateLimit);

        // Then
        verify(rateLimitService).tryAcquire(
                eq("BookController.getBookDetail(String):198.51.100.1"),
                eq(3),
                eq(10L)
        );
    }

    @Test
    @DisplayName("시간 단위 변환 테스트")
    void testTimeUnitConversion() throws Throwable {
        // Given - 1시간 = 3600초
        RateLimit rateLimit = createRateLimit(100, 1, TimeUnit.HOURS);

        when(rateLimitService.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        // When
        rateLimitAspect.handleRateLimit(joinPoint, rateLimit);

        // Then
        verify(rateLimitService).tryAcquire(anyString(), eq(100), eq(3600L));
    }

    @Test
    @DisplayName("커스텀 키 사용 테스트")
    void testCustomKey() throws Throwable {
        // Given
        RateLimit rateLimit = mock(RateLimit.class);
        lenient().when(rateLimit.limit()).thenReturn(5);
        lenient().when(rateLimit.window()).thenReturn(30L);
        lenient().when(rateLimit.timeUnit()).thenReturn(TimeUnit.SECONDS);
        lenient().when(rateLimit.keyType()).thenReturn(RateLimit.KeyType.IP);
        lenient().when(rateLimit.key()).thenReturn("#ip + ':' + #arg0"); // SpEL 표현식

        lenient().when(rateLimitService.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        lenient().when(joinPoint.proceed()).thenReturn("success");

        // When
        rateLimitAspect.handleRateLimit(joinPoint, rateLimit);

        // Then
        // 커스텀 키가 사용되어야 함 (SpEL 평가 결과)
        verify(rateLimitService).tryAcquire(
                contains("BookController.getBookDetail(String):192.168.1.1:9781234567890"),
                eq(5),
                eq(30L)
        );
    }

    @Test
    @DisplayName("Rate Limit Exception 속성 테스트")
    void testRateLimitExceptionProperties() {
        // Given
        int limit = 5;
        long windowInSeconds = 60;
        long remainingTime = 30;

        // When
        RateLimitExceededException exception = new RateLimitExceededException(limit, windowInSeconds, remainingTime);

        // Then
        assertThat(exception.getLimit()).isEqualTo(limit);
        assertThat(exception.getWindowInSeconds()).isEqualTo(windowInSeconds);
        assertThat(exception.getRemainingTime()).isEqualTo(remainingTime);
        assertThat(exception.getMessage())
                .contains("Rate limit exceeded")
                .contains("5 requests per 60 seconds")
                .contains("Try again in 30 seconds");
    }

    // Helper method to create RateLimit mock
    private RateLimit createRateLimit(int limit, long window, TimeUnit timeUnit) {
        return createRateLimit(limit, window, timeUnit, RateLimit.KeyType.IP);
    }

    private RateLimit createRateLimit(int limit, long window, TimeUnit timeUnit, RateLimit.KeyType keyType) {
        RateLimit rateLimit = mock(RateLimit.class);
        when(rateLimit.limit()).thenReturn(limit);
        when(rateLimit.window()).thenReturn(window);
        when(rateLimit.timeUnit()).thenReturn(timeUnit);
        when(rateLimit.keyType()).thenReturn(keyType);
        when(rateLimit.key()).thenReturn("");
        return rateLimit;
    }
}