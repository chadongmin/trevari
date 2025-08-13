package com.trevari.global.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting을 적용하기 위한 어노테이션
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * 허용되는 최대 요청 수
     */
    int limit() default 100;
    
    /**
     * 시간 윈도우 (기본 1분)
     */
    long window() default 1;
    
    /**
     * 시간 단위 (기본 분)
     */
    TimeUnit timeUnit() default TimeUnit.MINUTES;
    
    /**
     * Rate Limit 키 생성 전략
     * IP: IP 주소 기반
     * USER: 사용자 기반 (향후 인증 구현 시)
     * GLOBAL: 전역 제한
     */
    KeyType keyType() default KeyType.IP;
    
    /**
     * 커스텀 키 (SpEL 표현식 지원)
     */
    String key() default "";
    
    enum KeyType {
        IP, USER, GLOBAL
    }
}