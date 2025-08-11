package com.trevari.book.data;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 개발 시에만 사용하는 데이터 생성 러너
 * 
 * 사용법:
 * ./gradlew bootRun --args='--generate-data=true'
 * 
 * 주의: 운영 환경에서는 절대 사용하지 마세요!
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "generate-data", havingValue = "true")
public class DataGenerationRunner implements CommandLineRunner {
    
    private final GoogleBooksDataFetcher dataFetcher;
    
    @Override
    public void run(String... args) throws Exception {
        log.warn("=== DATA GENERATION MODE ===");
        log.warn("This will generate SQL INSERT statements from Google Books API");
        log.warn("DO NOT use this in production!");
        
        dataFetcher.generateSqlInsertStatements();
        
        log.warn("=== DATA GENERATION COMPLETE ===");
        log.warn("Copy the generated SQL to your data.sql file");
    }
}