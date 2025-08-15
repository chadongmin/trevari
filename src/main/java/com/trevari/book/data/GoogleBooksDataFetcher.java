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
            "Java programming", "Spring Boot", "Python development", "JavaScript tutorial", "React guide", "Angular framework", "Vue.js", "Node.js server", "TypeScript programming", "Kotlin development",
            "Database design", "MySQL administration", "PostgreSQL tutorial", "MongoDB guide", "Redis cache", "Elasticsearch search", "SQL queries", "NoSQL database", "DynamoDB AWS", "Firebase development",
            "Algorithm implementation", "Data Structure book", "Machine Learning practice", "AI artificial intelligence", "Deep Learning neural", "Neural Network guide", "Computer Vision opencv", "NLP processing", "Statistics analysis", "Mathematics programming",
            "Design Pattern software", "Clean Code practices", "Software Architecture", "Microservices design", "REST API development", "GraphQL tutorial", "Software Engineering principles", "Agile development", "DevOps practices", "Testing automation",
            "Cloud computing", "AWS services", "Microsoft Azure", "Google Cloud Platform", "Docker containers", "Kubernetes orchestration", "CI/CD pipeline", "Jenkins automation", "Git version control", "Linux administration",
            "Frontend development", "Backend programming", "Full Stack developer", "Mobile development", "Android programming", "iOS development", "Flutter framework", "React Native mobile", "Web Development guide", "API development",
            "Cybersecurity practices", "Information security", "Blockchain technology", "Encryption methods", "Network security", "System Design interview", "Distributed Systems design", "Performance optimization", "Scalability patterns", "System monitoring",
            "Programming fundamentals", "Software development", "Computer Science theory", "Technology trends", "Business intelligence", "Data Science analysis", "Analytics dashboard", "Big Data processing", "Data visualization", "ETL processes"
        );
        
        List<BookData> allBooks = new ArrayList<>();
        
        for (String keyword : keywords) {
            try {
                // 더 구체적인 검색으로 가격 정보가 있는 책 위주로 조회
                String url = String.format(
                    "https://www.googleapis.com/books/v1/volumes?q=%s&maxResults=15&langRestrict=en&orderBy=relevance&filter=paid-ebooks",
                    keyword
                );
                
                List<BookData> booksWithPrice = fetchBooksFromUrl(url);
                
                // 가격 정보가 없다면 일반 검색도 수행
                if (booksWithPrice.isEmpty()) {
                    url = String.format(
                        "https://www.googleapis.com/books/v1/volumes?q=%s&maxResults=10&langRestrict=en&orderBy=relevance",
                        keyword
                    );
                    booksWithPrice = fetchBooksFromUrl(url);
                }
                
                // 중복 제거하며 추가
                for (BookData book : booksWithPrice) {
                    if (!allBooks.contains(book)) {
                        allBooks.add(book);
                    }
                }
                
                // API 호출 간격 조절 (Google Books API rate limit 고려)
                Thread.sleep(150);
                
            } catch (Exception e) {
                log.error("Error fetching data for keyword: {}", keyword, e);
            }
        }
        
        // 가격 정보 보완
        allBooks = enhancePriceInformation(allBooks);
        
        log.info("Generated {} books for SQL insertion", allBooks.size());
        
        // SQL INSERT문 생성 및 파일에 직접 저장
        generateInsertStatementsToFile(allBooks);
    }
    
    /**
     * URL에서 도서 데이터를 가져오는 헬퍼 메서드
     */
    private List<BookData> fetchBooksFromUrl(String url) {
        List<BookData> books = new ArrayList<>();
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.get("items");
            
            if (items != null) {
                for (JsonNode item : items) {
                    BookData book = parseBookFromJson(item);
                    if (book != null) {
                        books.add(book);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error fetching books from URL: {}", url, e);
        }
        return books;
    }
    
    /**
     * 가격 정보가 없는 도서들에 대해 추가 정보를 보완하는 메서드
     */
    private List<BookData> enhancePriceInformation(List<BookData> books) {
        List<BookData> enhancedBooks = new ArrayList<>();
        
        for (BookData book : books) {
            if (book.amount() == null || book.currency() == null) {
                // 가격 정보가 없는 경우 추가 API 호출로 보완 시도
                BookData enhancedBook = tryEnhanceBookPrice(book);
                enhancedBooks.add(enhancedBook);
            } else {
                enhancedBooks.add(book);
            }
        }
        
        log.info("Enhanced price information for {} books", books.size());
        return enhancedBooks;
    }
    
    /**
     * 개별 도서의 가격 정보를 보완하는 메서드
     */
    private BookData tryEnhanceBookPrice(BookData originalBook) {
        try {
            // Google Books API에서 해당 ISBN으로 다시 조회
            String url = String.format(
                "https://www.googleapis.com/books/v1/volumes?q=isbn:%s",
                originalBook.isbn()
            );
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.get("items");
            
            if (items != null && items.size() > 0) {
                JsonNode item = items.get(0);
                JsonNode saleInfo = item.get("saleInfo");
                
                if (saleInfo != null && saleInfo.has("listPrice")) {
                    JsonNode listPrice = saleInfo.get("listPrice");
                    Integer amount = listPrice.has("amount") ? 
                        (int)Math.round(listPrice.get("amount").asDouble()) : null;
                    String currency = listPrice.has("currencyCode") ? 
                        listPrice.get("currencyCode").asText() : null;
                        
                    if (amount != null && currency != null) {
                        return new BookData(
                            originalBook.isbn(), originalBook.title(), originalBook.subtitle(),
                            originalBook.authors(), originalBook.publisher(), originalBook.publishedDate(),
                            originalBook.imageUrl(), originalBook.description(), originalBook.pageCount(),
                            originalBook.format(), originalBook.categories(), amount, currency
                        );
                    }
                }
            }
            
            // 가격 정보를 찾을 수 없다면 기본값 설정 (카테고리와 페이지 수 기반)
            return generateDefaultPrice(originalBook);
            
        } catch (Exception e) {
            log.debug("Failed to enhance price for book: {}", originalBook.isbn(), e);
            return generateDefaultPrice(originalBook);
        }
    }
    
    /**
     * 도서 특성에 기반한 기본 가격 생성
     */
    private BookData generateDefaultPrice(BookData book) {
        Integer defaultAmount = calculateDefaultPrice(book);
        String defaultCurrency = "USD";
        
        return new BookData(
            book.isbn(), book.title(), book.subtitle(),
            book.authors(), book.publisher(), book.publishedDate(),
            book.imageUrl(), book.description(), book.pageCount(),
            book.format(), book.categories(), defaultAmount, defaultCurrency
        );
    }
    
    /**
     * 도서 특성에 기반한 가격 계산
     */
    private Integer calculateDefaultPrice(BookData book) {
        int basePrice = 2999; // 기본 가격 $29.99
        
        // 페이지 수에 따른 가격 조정
        if (book.pageCount() != null) {
            if (book.pageCount() > 500) {
                basePrice += 1500; // +$15 for thick books
            } else if (book.pageCount() > 300) {
                basePrice += 1000; // +$10 for medium books
            }
        }
        
        // 카테고리에 따른 가격 조정
        if (book.categories() != null && !book.categories().isEmpty()) {
            String firstCategory = book.categories().get(0).toLowerCase();
            if (firstCategory.contains("computer") || firstCategory.contains("technology")) {
                basePrice += 500; // 기술 서적은 더 비쌈
            } else if (firstCategory.contains("business") || firstCategory.contains("management")) {
                basePrice += 800;
            }
        }
        
        // 출간년도에 따른 가격 조정
        if (book.publishedDate() != null) {
            int yearsSincePublished = LocalDate.now().getYear() - book.publishedDate().getYear();
            if (yearsSincePublished > 5) {
                basePrice = (int)(basePrice * 0.8); // 5년 이상 된 책은 20% 할인
            } else if (yearsSincePublished <= 1) {
                basePrice = (int)(basePrice * 1.2); // 신간은 20% 프리미엄
            }
        }
        
        // 최소/최대 가격 제한
        return Math.max(999, Math.min(basePrice, 9999)); // $9.99 ~ $99.99
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
