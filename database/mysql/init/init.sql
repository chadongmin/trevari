CREATE DATABASE IF NOT EXISTS trevari CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE trevari;

-- Book 엔티티에 대응하는 테이블
CREATE TABLE book (
    isbn VARCHAR(20) PRIMARY KEY COMMENT 'ISBN',
    title VARCHAR(500) NOT NULL,
    subtitle VARCHAR(500),
    publisher VARCHAR(255) NOT NULL,
    published_date DATE NOT NULL,
    INDEX idx_title (title),
    INDEX idx_publisher (publisher),
    INDEX idx_published_date (published_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE book_authors (
    book_isbn VARCHAR(20) NOT NULL,
    authors VARCHAR(255) NOT NULL,
    FOREIGN KEY (book_isbn) REFERENCES book(isbn) ON DELETE CASCADE,
    INDEX idx_authors (authors)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE search_keywords (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(255) NOT NULL,
    search_count BIGINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_keyword (keyword),
    INDEX idx_search_count (search_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;