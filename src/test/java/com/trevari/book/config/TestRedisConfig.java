package com.trevari.book.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 테스트용 캐시 설정
 * Redis 대신 메모리 기반 캐시 사용
 */
@Configuration
@Profile("test")
public class TestRedisConfig {

    @Bean
    @Primary
    public CacheManager testCacheManager() {
        return new ConcurrentMapCacheManager("bookSearch", "bookDetail", "popularKeywords");
    }
}