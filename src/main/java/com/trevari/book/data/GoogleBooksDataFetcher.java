package com.trevari.book.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
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
     * 기존 데이터를 복제하여 대량의 SQL INSERT문을 빠르게 생성
     * 이 메서드는 개발 시에 한번만 실행해서 data.sql 파일을 생성할 용도
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
            
            // 이미지 URL 추출
            String imageUrl = extractImageUrl(volumeInfo.get("imageLinks"));
            
            return new BookData(isbn, title, subtitle, authors, publisher, publishedDate, imageUrl);
            
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
    
    private String extractImageUrl(JsonNode imageLinks) {
        if (imageLinks == null) return null;
        
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
    
    private void generateInsertStatementsToFile(List<BookData> books) {
        StringBuilder sql = new StringBuilder();
        sql.append("-- Generated book data with image URLs\n");
        sql.append("-- DO NOT run this in production - use pre-generated data.sql instead\n\n");
        
        // Book 테이블 INSERT문
        sql.append("-- Book data with realistic book cover images\n");
        for (BookData book : books) {
            sql.append(String.format(
                "INSERT INTO book (isbn, title, subtitle, publisher, published_date, image_url) VALUES ('%s', '%s', %s, '%s', '%s', %s);\n",
                escapeSql(book.isbn()),
                escapeSql(book.title()),
                book.subtitle() != null ? "'" + escapeSql(book.subtitle()) + "'" : "NULL",
                escapeSql(book.publisher()),
                book.publishedDate(),
                book.imageUrl() != null ? "'" + escapeSql(book.imageUrl()) + "'" : "NULL"
            ));
        }
        
        sql.append("\n-- Book authors\n");
        for (BookData book : books) {
            for (String author : book.authors()) {
                sql.append(String.format(
                    "INSERT INTO book_authors (book_isbn, authors) VALUES ('%s', '%s');\n",
                    escapeSql(book.isbn()),
                    escapeSql(author)
                ));
            }
        }
        
        // Search keywords 데이터 추가
        sql.append("\n-- Search keywords data\n");
        String[] keywords = {"Java", "Spring", "Python", "JavaScript", "React", "Database", "Algorithm", 
                           "Clean Code", "Design Pattern", "Architecture", "Microservices", "Kubernetes", 
                           "MySQL", "MongoDB", "Programming"};
        int count = 150;
        for (String keyword : keywords) {
            sql.append(String.format(
                "INSERT INTO search_keywords (keyword, search_count) VALUES ('%s', %d);\n",
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
            // 콘솔에도 출력
            log.info("Generated SQL INSERT statements:\n{}", sql.toString());
        }
    }
    
    private List<BookData> createSampleBooks() {
        return Arrays.asList(
            new BookData("9781617297397", "Java in Action", "Lambdas, streams, functional and reactive programming", 
                       Arrays.asList("Raoul-Gabriel Urma", "Mario Fusco", "Alan Mycroft"), "Manning Publications", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=0pkyCwAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"),
            new BookData("9781449370831", "Java Pocket Guide", "Instant Access to Java Fundamentals", 
                       Arrays.asList("Robert Liguori", "Patricia Liguori"), "O'Reilly Media", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=1AdnCwAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"),
            new BookData("9781492056300", "Head First Java", "A Brain-Friendly Guide", 
                       Arrays.asList("Kathy Sierra", "Bert Bates"), "O'Reilly Media", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=4P2uDwAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"),
            new BookData("9781617294945", "Spring in Action", "Fifth Edition", 
                       Arrays.asList("Craig Walls"), "Manning Publications", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=p1rMDwAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"),
            new BookData("9781617293290", "Spring Boot in Action", null, 
                       Arrays.asList("Craig Walls"), "Manning Publications", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=tOiPCwAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"),
            new BookData("9781491950401", "Learning Python", "Powerful Object-Oriented Programming", 
                       Arrays.asList("Mark Lutz"), "O'Reilly Media", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=ftA0DwAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"),
            new BookData("9781449357016", "Fluent Python", "Clear, Concise, and Effective Programming", 
                       Arrays.asList("Luciano Ramalho"), "O'Reilly Media", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=bIxWCgAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"),
            new BookData("9781593279288", "Automate the Boring Stuff with Python", "Practical Programming for Total Beginners", 
                       Arrays.asList("Al Sweigart"), "No Starch Press", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=_StSCgAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"),
            new BookData("9781449331818", "Learning JavaScript Design Patterns", "A JavaScript and jQuery Developer's Guide", 
                       Arrays.asList("Addy Osmani"), "O'Reilly Media", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=ka2VUBqHiWkC&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"),
            new BookData("9781492037255", "React: Up & Running", "Building Web Applications", 
                       Arrays.asList("Stoyan Stefanov"), "O'Reilly Media", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=LC8gDgAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"),
            new BookData("9781491954324", "Learning React", "Functional Web Development with React and Redux", 
                       Arrays.asList("Alex Banks", "Eve Porcello"), "O'Reilly Media", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=IOmLDAAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"),
            new BookData("9781449373320", "Designing Data-Intensive Applications", "The Big Ideas Behind Reliable, Scalable, and Maintainable Systems", 
                       Arrays.asList("Martin Kleppmann"), "O'Reilly Media", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=p1hSDgAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"),
            new BookData("9780134685991", "Clean Code", "A Handbook of Agile Software Craftsmanship", 
                       Arrays.asList("Robert C. Martin"), "Prentice Hall", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=hjEFCAAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"),
            new BookData("9780201633610", "Design Patterns", "Elements of Reusable Object-Oriented Software", 
                       Arrays.asList("Erich Gamma", "Richard Helm", "Ralph Johnson", "John Vlissides"), "Addison-Wesley Professional", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=6oHuKQe3TjQC&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"),
            new BookData("9781617290541", "Microservices Patterns", "With examples in Java", 
                       Arrays.asList("Chris Richardson"), "Manning Publications", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=WPjqDwAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"),
            new BookData("9781492032640", "Building Microservices", "Designing Fine-Grained Systems", 
                       Arrays.asList("Sam Newman"), "O'Reilly Media", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=jjl4BgAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"),
            new BookData("9781492046523", "Kubernetes: Up and Running", "Dive into the Future of Infrastructure", 
                       Arrays.asList("Kelsey Hightower", "Brendan Burns", "Joe Beda"), "O'Reilly Media", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=JdXvDwAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"),
            new BookData("9781449367640", "MongoDB: The Definitive Guide", "Powerful and Scalable Data Storage", 
                       Arrays.asList("Kristina Chodorow"), "O'Reilly Media", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=f0MUAAAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"),
            new BookData("9780262033848", "Introduction to Algorithms", "Third Edition", 
                       Arrays.asList("Thomas H. Cormen", "Charles E. Leiserson", "Ronald L. Rivest", "Clifford Stein"), "MIT Press", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=VK14QgAACAAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api"),
            new BookData("9780321356680", "Effective Java", "Second Edition", 
                       Arrays.asList("Joshua Bloch"), "Addison-Wesley Professional", 
                       LocalDate.of(2020, 1, 1), "https://books.google.com/books/content?id=ka2VUBqHiWkC&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api")
        );
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
        LocalDate publishedDate,
        String imageUrl
    ) {}
}