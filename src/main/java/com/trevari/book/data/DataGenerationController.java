package com.trevari.book.data;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개발용 데이터 생성 컨트롤러
 * 운영 환경에서는 사용하지 않음
 */
@Slf4j
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DataGenerationController {

    private final GoogleBooksDataFetcher googleBooksDataFetcher;

    /**
     * Google Books API에서 데이터를 가져와 02_data.sql 파일 생성
     * 
     * POST /api/dev/generate-data
     */
    @PostMapping("/generate-data")
    public ResponseEntity<String> generateSqlData() {
        try {
            log.info("Starting data generation from Google Books API...");
            googleBooksDataFetcher.generateSqlInsertStatements();
            log.info("Data generation completed successfully");
            
            return ResponseEntity.ok()
                .body("Data generation completed successfully. Check database/mysql/init/02_data.sql file.");
                
        } catch (Exception e) {
            log.error("Failed to generate data", e);
            return ResponseEntity.internalServerError()
                .body("Failed to generate data: " + e.getMessage());
        }
    }
}