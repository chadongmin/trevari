package com.trevari.global.ratelimit;

/**
 * Rate Limit이 초과되었을 때 발생하는 예외
 */
public class RateLimitExceededException extends RuntimeException {
    
    private final int limit;
    private final long windowInSeconds;
    private final long remainingTime;
    
    public RateLimitExceededException(int limit, long windowInSeconds, long remainingTime) {
        super(String.format("Rate limit exceeded. Limit: %d requests per %d seconds. Try again in %d seconds.", 
                limit, windowInSeconds, remainingTime));
        this.limit = limit;
        this.windowInSeconds = windowInSeconds;
        this.remainingTime = remainingTime;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public long getWindowInSeconds() {
        return windowInSeconds;
    }
    
    public long getRemainingTime() {
        return remainingTime;
    }
}