package com.trevari.global.ratelimit;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * Redis를 이용한 Rate Limiting 서비스 Sliding Window Counter 알고리즘 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("!test")
public class RateLimitService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Lua 스크립트를 사용한 원자적 Rate Limiting 체크 Sliding Window Counter 알고리즘 구현
     */
    private static final String RATE_LIMIT_LUA_SCRIPT = """
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
        """;
    
    private final DefaultRedisScript<List> rateLimitScript = new DefaultRedisScript<>(RATE_LIMIT_LUA_SCRIPT, List.class);
    
    /**
     * Rate Limit 체크 및 적용
     *
     * @param key           제한을 적용할 키
     * @param limit         허용 요청 수
     * @param windowSeconds 시간 윈도우 (초)
     * @return 요청이 허용되면 true
     * @throws RateLimitExceededException Rate Limit 초과 시
     */
    public boolean tryAcquire(String key, int limit, long windowSeconds) {
        String redisKey = "rate_limit:" + key;
        long currentTime = System.currentTimeMillis() / 1000; // Unix timestamp in seconds
        
        try {
            @SuppressWarnings("unchecked")
            List<Long> result = redisTemplate.execute(
                rateLimitScript,
                List.of(redisKey),
                windowSeconds, limit, currentTime
            );
            
            if (result.size() >= 2) {
                boolean allowed = result.get(0) == 1L;
                long remaining = result.get(1);
                
                if (!allowed) {
                    // Rate limit 초과 시 남은 시간 계산
                    long remainingTime = calculateRemainingTime(redisKey, windowSeconds);
                    log.warn("Rate limit exceeded for key: {}, limit: {}, window: {}s", key, limit, windowSeconds);
                    throw new RateLimitExceededException(limit, windowSeconds, remainingTime);
                }
                
                log.debug("Rate limit check passed for key: {}, remaining: {}", key, remaining);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            if (e instanceof RateLimitExceededException) {
                throw e;
            }
            log.error("Error checking rate limit for key: {}", key, e);
            // Redis 오류 시 요청 허용 (fail-open 정책)
            return true;
        }
    }
    
    /**
     * Rate Limit 정보 조회
     */
    public RateLimitInfo getRateLimitInfo(String key, long windowSeconds) {
        String redisKey = "rate_limit:" + key;
        long currentTime = System.currentTimeMillis() / 1000;
        long windowStart = currentTime - windowSeconds;
        
        try {
            // 만료된 요청들 제거
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);
            
            // 현재 윈도우 내의 요청 수
            Long currentRequests = redisTemplate.opsForZSet().zCard(redisKey);
            long used = currentRequests != null ? currentRequests : 0;
            
            return new RateLimitInfo(used, windowSeconds);
        } catch (Exception e) {
            log.error("Error getting rate limit info for key: {}", key, e);
            return new RateLimitInfo(0, windowSeconds);
        }
    }
    
    private long calculateRemainingTime(String redisKey, long windowSeconds) {
        try {
            // 가장 오래된 요청의 시간을 조회하여 남은 시간 계산
            var oldest = redisTemplate.opsForZSet().range(redisKey, 0, 0);
            if (oldest != null && !oldest.isEmpty()) {
                long oldestTime = Long.parseLong(oldest.iterator().next().toString());
                long currentTime = System.currentTimeMillis() / 1000;
                return Math.max(0, windowSeconds - (currentTime - oldestTime));
            }
        } catch (Exception e) {
            log.debug("Error calculating remaining time", e);
        }
        return windowSeconds; // 기본값으로 전체 윈도우 시간 반환
    }
    
    /**
     * Rate Limit 정보를 담는 클래스
     */
    public static class RateLimitInfo {
        
        private final long currentRequests;
        private final long windowSeconds;
        
        public RateLimitInfo(long currentRequests, long windowSeconds) {
            this.currentRequests = currentRequests;
            this.windowSeconds = windowSeconds;
        }
        
        public long getCurrentRequests() {
            return currentRequests;
        }
        
        public long getWindowSeconds() {
            return windowSeconds;
        }
    }
}