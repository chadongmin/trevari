package com.trevari.global.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting AOP Aspect
 * @RateLimit 어노테이션이 적용된 메서드에 대해 Rate Limiting을 수행
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {
    
    private final RateLimitService rateLimitService;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    
    public RateLimitAspect(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
        System.out.println(">>> RATE LIMIT ASPECT BEAN CREATED <<<");
        log.error(">>> RATE LIMIT ASPECT BEAN CREATED <<<");
    }
    
    @Around("@annotation(rateLimit)")
    public Object handleRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        System.out.println(">>> RATE LIMIT ASPECT TRIGGERED <<<");
        log.error(">>> RATE LIMIT ASPECT TRIGGERED for method: {} <<<", joinPoint.getSignature().getName());
        
        String key = generateKey(joinPoint, rateLimit);
        long windowInSeconds = rateLimit.timeUnit().toSeconds(rateLimit.window());
        
        System.out.println(">>> Rate Limit Check - key: " + key + ", limit: " + rateLimit.limit() + ", window: " + windowInSeconds + "s");
        log.error(">>> Rate Limit Check - key: {}, limit: {}, window: {}s <<<", key, rateLimit.limit(), windowInSeconds);
        
        // Rate limit 체크
        rateLimitService.tryAcquire(key, rateLimit.limit(), windowInSeconds);
        
        // Rate limit 통과 시 원래 메서드 실행
        return joinPoint.proceed();
    }
    
    /**
     * Rate Limit 키 생성
     */
    private String generateKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String baseKey = joinPoint.getSignature().toShortString();
        
        // 커스텀 키가 설정된 경우
        if (StringUtils.hasText(rateLimit.key())) {
            return baseKey + ":" + evaluateKey(rateLimit.key(), joinPoint);
        }
        
        // 키 타입에 따른 키 생성
        switch (rateLimit.keyType()) {
            case IP:
                return baseKey + ":" + getClientIP();
            case USER:
                // 향후 인증 구현 시 사용자 ID 사용
                return baseKey + ":" + getClientIP(); // 현재는 IP로 대체
            case GLOBAL:
                return baseKey + ":global";
            default:
                return baseKey + ":" + getClientIP();
        }
    }
    
    /**
     * SpEL 표현식을 사용한 키 평가
     */
    private String evaluateKey(String keyExpression, ProceedingJoinPoint joinPoint) {
        try {
            EvaluationContext context = new StandardEvaluationContext();
            
            // 메서드 파라미터를 컨텍스트에 추가
            String[] paramNames = getParameterNames(joinPoint);
            Object[] args = joinPoint.getArgs();
            
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
            
            // 추가 변수들
            context.setVariable("ip", getClientIP());
            context.setVariable("method", joinPoint.getSignature().getName());
            
            Object result = expressionParser.parseExpression(keyExpression).getValue(context);
            return result != null ? result.toString() : "";
        } catch (Exception e) {
            log.warn("Error evaluating SpEL expression: {}", keyExpression, e);
            return getClientIP(); // 기본값으로 IP 사용
        }
    }
    
    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIP() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            
            // X-Forwarded-For 헤더 확인 (프록시를 통한 요청인 경우)
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(xForwardedFor)) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            // X-Real-IP 헤더 확인
            String xRealIP = request.getHeader("X-Real-IP");
            if (StringUtils.hasText(xRealIP)) {
                return xRealIP;
            }
            
            // 기본적으로 Remote Address 사용
            return request.getRemoteAddr();
        } catch (Exception e) {
            log.debug("Error getting client IP", e);
            return "unknown";
        }
    }
    
    /**
     * 메서드 파라미터 이름 추출 (간단한 구현)
     */
    private String[] getParameterNames(ProceedingJoinPoint joinPoint) {
        // 실제 구현에서는 더 정교한 파라미터 이름 추출이 필요할 수 있음
        int paramCount = joinPoint.getArgs().length;
        String[] paramNames = new String[paramCount];
        for (int i = 0; i < paramCount; i++) {
            paramNames[i] = "arg" + i;
        }
        return paramNames;
    }
}