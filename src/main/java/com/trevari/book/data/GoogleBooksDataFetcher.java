package com.trevari.book.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Google Books API에서 데이터를 가져와서 SQL INSERT문을 생성하는 유틸리티
 * 개발 시에만 사용하며, 운영에서는 사용하지 않음
 */
@Slf4j
@Component
public class GoogleBooksDataFetcher {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Google Books API에서 데이터를 가져와서 SQL INSERT문을 생성
     * 이 메서드는 개발 시에 한번만 실행해서 data.sql 파일을 생성할 용도
     */
    public void generateSqlInsertStatements() {
        List<String> keywords = List.of(
            "Java", "Spring", "Python", "JavaScript", "React", 
            "Database", "Algorithm", "Design Pattern", "Clean Code", "Architecture"
        );
        
        List<BookData> allBooks = new ArrayList<>();
        
        for (String keyword : keywords) {
            try {
                String url = String.format(
                    "https://www.googleapis.com/books/v1/volumes?q=%s&maxResults=10&langRestrict=en",
                    keyword
                );
                
                String response = restTemplate.getForObject(url, String.class);
                JsonNode root = objectMapper.readTree(response);
                JsonNode items = root.get("items");
                
                if (items != null) {
                    for (JsonNode item : items) {
                        BookData book = parseBookFromJson(item);
                        if (book != null && !allBooks.contains(book)) {
                            allBooks.add(book);
                        }
                    }
                }
                
                // API 호출 간격 조절
                Thread.sleep(100);
                
            } catch (Exception e) {
                log.error("Error fetching data for keyword: {}", keyword, e);
            }
        }
        
        // SQL INSERT문 생성
        generateInsertStatements(allBooks);
    }
    
    private BookData parseBookFromJson(JsonNode item) {
        try {
            JsonNode volumeInfo = item.get("volumeInfo");
            if (volumeInfo == null) return null;
            
            // ISBN 추출
            String isbn = extractIsbn(volumeInfo.get("industryIdentifiers"));
            if (isbn == null) {
                // ISBN이 없으면 Google Books ID 사용
                isbn = item.get("id").asText();
            }
            
            String title = volumeInfo.has("title") ? 
                volumeInfo.get("title").asText() : "Unknown Title";
            
            String subtitle = volumeInfo.has("subtitle") ? 
                volumeInfo.get("subtitle").asText() : null;
            
            List<String> authors = new ArrayList<>();
            if (volumeInfo.has("authors")) {
                for (JsonNode author : volumeInfo.get("authors")) {
                    authors.add(author.asText());
                }
            }
            
            String publisher = volumeInfo.has("publisher") ? 
                volumeInfo.get("publisher").asText() : "Unknown Publisher";
            
            LocalDate publishedDate = parsePublishedDate(
                volumeInfo.has("publishedDate") ? volumeInfo.get("publishedDate").asText() : null
            );
            
            return new BookData(isbn, title, subtitle, authors, publisher, publishedDate);
            
        } catch (Exception e) {
            log.error("Error parsing book data", e);
            return null;
        }
    }
    
    private String extractIsbn(JsonNode identifiers) {
        if (identifiers == null) return null;
        
        for (JsonNode identifier : identifiers) {
            String type = identifier.get("type").asText();
            if ("ISBN_13".equals(type) || "ISBN_10".equals(type)) {
                return identifier.get("identifier").asText();
            }
        }
        return null;
    }
    
    private LocalDate parsePublishedDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDate.of(2020, 1, 1); // 기본값
        }
        
        try {
            if (dateStr.length() == 4) { // 년도만
                return LocalDate.of(Integer.parseInt(dateStr), 1, 1);
            } else if (dateStr.length() == 7) { // YYYY-MM
                String[] parts = dateStr.split("-");
                return LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 1);
            } else { // YYYY-MM-DD
                return LocalDate.parse(dateStr);
            }
        } catch (Exception e) {
            return LocalDate.of(2020, 1, 1);
        }
    }
    
    private void generateInsertStatements(List<BookData> books) {
        StringBuilder sql = new StringBuilder();
        sql.append("-- Generated book data from Google Books API\n");
        sql.append("-- DO NOT run this in production - use pre-generated data.sql instead\n\n");
        
        // Book 테이블 INSERT문
        sql.append("-- Book data\n");
        for (BookData book : books) {
            sql.append(String.format(
                "INSERT INTO book (isbn, title, subtitle, publisher, published_date) VALUES ('%s', '%s', %s, '%s', '%s');\n",
                escapeSql(book.isbn()),
                escapeSql(book.title()),
                book.subtitle() != null ? "'" + escapeSql(book.subtitle()) + "'" : "NULL",
                escapeSql(book.publisher()),
                book.publishedDate()
            ));
        }
        
        sql.append("\n-- Book authors data\n");
        for (BookData book : books) {
            for (String author : book.authors()) {
                sql.append(String.format(
                    "INSERT INTO book_authors (book_isbn, authors) VALUES ('%s', '%s');\n",
                    escapeSql(book.isbn()),
                    escapeSql(author)
                ));
            }
        }
        
        log.info("Generated SQL INSERT statements:");
        log.info("\n{}", sql.toString());
    }
    
    private String escapeSql(String value) {
        if (value == null) return "";
        return value.replace("'", "''").replace("\n", " ").replace("\r", "");
    }
    
    private record BookData(
        String isbn,
        String title, 
        String subtitle,
        List<String> authors,
        String publisher,
        LocalDate publishedDate
    ) {}
}