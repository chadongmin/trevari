package com.trevari.book.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Google Books API에서 데이터를 가져와서 SQL INSERT문을 생성하는 유틸리티 개발 시에만 사용하며, 운영에서는 사용하지 않음
 */
@Slf4j
@Component
public class GoogleBooksDataFetcher {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 기존 데이터를 복제하여 대량의 SQL INSERT문을 빠르게 생성 이 메서드는 개발 시에 한번만 실행해서 data.sql 파일을 생성할 용도
     */
    public void generateSqlInsertStatements() {
        List<String> keywords = List.of(
            "Java", "Spring", "Python", "JavaScript", "React", "Angular", "Vue", "Node.js", "TypeScript", "Kotlin",
            "Database", "MySQL", "PostgreSQL", "MongoDB", "Redis", "Elasticsearch", "SQL", "NoSQL", "DynamoDB", "Firebase",
            "Algorithm", "Data Structure", "Machine Learning", "AI", "Deep Learning", "Neural Network", "Computer Vision", "NLP", "Statistics", "Math",
            "Design Pattern", "Clean Code", "Architecture", "Microservices", "REST API", "GraphQL", "Software Engineering", "Agile", "DevOps", "Testing",
            "Cloud", "AWS", "Azure", "Google Cloud", "Docker", "Kubernetes", "CI/CD", "Jenkins", "Git", "Linux",
            "Frontend", "Backend", "Full Stack", "Mobile", "Android", "iOS", "Flutter", "React Native", "Web Development", "API",
            "Security", "Cybersecurity", "Blockchain", "Encryption", "Network", "System Design", "Distributed Systems", "Performance", "Scalability", "Monitoring",
            "Programming", "Coding", "Software", "Computer Science", "Technology", "Innovation", "Startup", "Business", "Management", "Leadership",
            "Data Science", "Analytics", "Big Data", "Visualization", "Business Intelligence", "ETL", "Data Engineering", "Apache", "Spark", "Hadoop",
            "Game Development", "Unity", "Graphics", "3D", "VR", "AR", "Robotics", "IoT", "Embedded", "Hardware"
        );
        
        List<BookData> allBooks = new ArrayList<>();
        
        for (String keyword : keywords) {
            try {
                String url = String.format(
                    "https://www.googleapis.com/books/v1/volumes?q=%s&maxResults=20&langRestrict=en",
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
                
                // API 호출 간격 조절 (Google Books API rate limit 고려)
                Thread.sleep(100);
                
            } catch (Exception e) {
                log.error("Error fetching data for keyword: {}", keyword, e);
            }
        }
        
        log.info("Generated {} books for SQL insertion", allBooks.size());
        
        // SQL INSERT문 생성 및 파일에 직접 저장
        generateInsertStatementsToFile(allBooks);
    }
    
    private BookData parseBookFromJson(JsonNode item) {
        try {
            JsonNode volumeInfo = item.get("volumeInfo");
            if (volumeInfo == null) {
                return null;
            }
            
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
            
            // 이미지 URL 추출
            String imageUrl = extractImageUrl(volumeInfo.get("imageLinks"));
            
            String description = volumeInfo.has("description") ? volumeInfo.get("description").asText() : null;
            Integer pageCount = volumeInfo.has("pageCount") ? volumeInfo.get("pageCount").asInt() : null;
            String format = volumeInfo.has("printType") ? volumeInfo.get("printType").asText() : null;
            
            List<String> categories = new ArrayList<>();
            if (volumeInfo.has("categories")) {
                for (JsonNode categoryNode : volumeInfo.get("categories")) {
                    // "Computers / Programming" 같은 경우 "Computers"만 저장
                    categories.add(categoryNode.asText().split(" / ")[0]);
                }
            }
            
            JsonNode saleInfo = item.get("saleInfo");
            Integer amount = null;
            String currency = null;
            if (saleInfo != null && saleInfo.has("listPrice")) {
                JsonNode listPrice = saleInfo.get("listPrice");
                // amount는 스키마에서 INT이므로 Double -> Integer로 변환
                amount = listPrice.has("amount") ? (int)Math.round(listPrice.get("amount").asDouble()) : null;
                currency = listPrice.has("currencyCode") ? listPrice.get("currencyCode").asText() : null;
            }
            
            return new BookData(isbn, title, subtitle, authors, publisher, publishedDate, imageUrl, description, pageCount, format, categories, amount, currency);
            
        } catch (Exception e) {
            log.error("Error parsing book data", e);
            return null;
        }
    }
    
    private String extractIsbn(JsonNode identifiers) {
        if (identifiers == null) {
            return null;
        }
        
        for (JsonNode identifier : identifiers) {
            String type = identifier.get("type").asText();
            if ("ISBN_13".equals(type) || "ISBN_10".equals(type)) {
                return identifier.get("identifier").asText();
            }
        }
        return null;
    }
    
    private String extractImageUrl(JsonNode imageLinks) {
        if (imageLinks == null) {
            return null;
        }
        
        // 선호도 순서: thumbnail > smallThumbnail > small > medium > large
        if (imageLinks.has("thumbnail")) {
            return imageLinks.get("thumbnail").asText();
        } else if (imageLinks.has("smallThumbnail")) {
            return imageLinks.get("smallThumbnail").asText();
        } else if (imageLinks.has("small")) {
            return imageLinks.get("small").asText();
        } else if (imageLinks.has("medium")) {
            return imageLinks.get("medium").asText();
        } else if (imageLinks.has("large")) {
            return imageLinks.get("large").asText();
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
    
    // GoogleBooksDataFetcher.java 파일의 generateInsertStatementsToFile 메서드를 아래 코드로 교체하세요.
    
    private void generateInsertStatementsToFile(List<BookData> books) {
        StringBuilder sql = new StringBuilder();
        sql.append("-- Generated book data, compatible with 01_schema.sql\n");
        sql.append("-- DO NOT run this in production - use pre-generated data.sql instead\n\n");
        
        // Collect unique authors and categories
        Set<String> allAuthors = new HashSet<>();
        Set<String> allCategories = new HashSet<>();
        for (BookData book : books) {
            if (book.authors() != null) {
                allAuthors.addAll(book.authors());
            }
            if (book.categories() != null) {
                allCategories.addAll(book.categories());
            }
        }
        
        // Author table INSERTs
        sql.append("-- Authors\n");
        allAuthors.stream()
            .filter(author -> author != null && !author.isBlank() && author.length() <= 255)
            .forEach(author -> sql.append(String.format("INSERT IGNORE INTO author (name) VALUES ('%s');\n", escapeSql(author))));
        sql.append("\n");
        
        // Category table INSERTs
        sql.append("-- Categories\n");
        allCategories.stream()
            .filter(cat -> cat != null && !cat.isBlank() && cat.length() <= 255)
            .forEach(category -> sql.append(String.format("INSERT IGNORE INTO category (name) VALUES ('%s');\n", escapeSql(category))));
        sql.append("\n");
        
        // Book table INSERTs
        sql.append("-- Books\n");
        for (BookData book : books) {
            sql.append(String.format(
                "INSERT IGNORE INTO book (isbn, title, subtitle, description, page_count, format, amount, currency, publisher, published_date, image_url) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);\n",
                toSqlString(book.isbn()),
                toSqlString(book.title()),
                toSqlString(book.subtitle()),
                toSqlString(book.description()),
                toSqlValue(book.pageCount()),
                toSqlString(book.format()),
                toSqlValue(book.amount()),
                toSqlString(book.currency()),
                toSqlString(book.publisher()),
                book.publishedDate() != null ? "'" + book.publishedDate().toString() + "'" : "NULL", // 날짜는 직접 처리
                toSqlString(book.imageUrl())
            ));
        }
        sql.append("\n");
        
        // ... 나머지 book_author, book_category, search_keywords 생성 로직은 동일 ...
        // (이하 코드는 기존과 동일하게 유지)
        
        // Book-Author join table INSERTs
        sql.append("-- Book-Author relationships\n");
        for (BookData book : books) {
            if (book.authors() == null) {
                continue;
            }
            book.authors().stream()
                .filter(author -> author != null && !author.isBlank() && author.length() <= 255)
                .forEach(author -> sql.append(String.format(
                    "INSERT IGNORE INTO book_author (book_isbn, author_id, role) SELECT '%s', id, '저자' FROM author WHERE name = '%s';\n",
                    escapeSql(book.isbn()),
                    escapeSql(author)
                )));
        }
        sql.append("\n");
        
        // Book-Category join table INSERTs
        sql.append("-- Book-Category relationships\n");
        for (BookData book : books) {
            if (book.categories() == null) {
                continue;
            }
            book.categories().stream()
                .filter(cat -> cat != null && !cat.isBlank() && cat.length() <= 255)
                .forEach(category -> sql.append(String.format(
                    "INSERT IGNORE INTO book_category (book_isbn, category_id) SELECT '%s', id FROM category WHERE name = '%s';\n",
                    escapeSql(book.isbn()),
                    escapeSql(category)
                )));
        }
        sql.append("\n");
        
        // Search keywords data
        sql.append("-- Search keywords data\n");
        String[] keywordsArray = {"Java", "Spring", "Python", "JavaScript", "React", "Database", "Algorithm",
            "Clean Code", "Design Pattern", "Architecture", "Microservices", "Kubernetes",
            "MySQL", "MongoDB", "Programming"};
        int count = 150;
        for (String keyword : keywordsArray) {
            sql.append(String.format(
                "INSERT IGNORE INTO search_keywords (keyword, search_count) VALUES ('%s', %d);\n",
                escapeSql(keyword), count
            ));
            count -= 10;
        }
        
        // 파일에 직접 저장
        try {
            String filePath = "database/mysql/init/02_data.sql";
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(sql.toString());
            }
            log.info("Successfully generated {} books and saved to {}", books.size(), filePath);
        } catch (IOException e) {
            log.error("Failed to write SQL file", e);
            log.info("Generated SQL INSERT statements:\n{}", sql.toString());
        }
    }
    
    // 아래 두 개의 헬퍼 메서드를 추가해주세요.
    private String toSqlString(String value) {
        if (value == null) {
            return "NULL";
        }
        return "'" + escapeSql(value) + "'";
    }
    
    private String toSqlValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        return value.toString();
    }
    
    // escapeSql 메서드는 기존의 것을 그대로 사용합니다.
    private String escapeSql(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
            .replace("\\", "\\\\") // 역슬래시 먼저 처리
            .replace("'", "''")
            .replace("\n", " ")
            .replace("\r", "");
    }
    
    
    private record BookData(
        String isbn,
        String title,
        String subtitle,
        List<String> authors,
        String publisher,
        LocalDate publishedDate,
        String imageUrl,
        String description,
        Integer pageCount,
        String format,
        List<String> categories,
        Integer amount,
        String currency
    ) {
    
    }
}
