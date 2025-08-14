package com.trevari.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson 설정 클래스
 * API 응답용 ObjectMapper를 설정합니다.
 */
@Configuration
public class JacksonConfig {

    /**
     * API 응답용 ObjectMapper Bean 생성
     * Redis와 달리 타입 정보를 포함하지 않습니다.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // API 응답에는 타입 정보를 포함하지 않음
        return objectMapper;
    }
}