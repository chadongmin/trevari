-- Create database tables
CREATE TABLE IF NOT EXISTS book (
    isbn VARCHAR(255) NOT NULL PRIMARY KEY,
    title VARCHAR(1000) NOT NULL,
    subtitle VARCHAR(1000),
    publisher VARCHAR(500) NOT NULL,
    published_date DATE NOT NULL,
    image_url VARCHAR(1000)
);

CREATE TABLE IF NOT EXISTS book_authors (
    book_isbn VARCHAR(255) NOT NULL,
    authors VARCHAR(500) NOT NULL,
    KEY book_authors_book_isbn (book_isbn),
    CONSTRAINT fk_book_authors_book_isbn 
        FOREIGN KEY (book_isbn) REFERENCES book (isbn) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS search_keywords (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(255) NOT NULL UNIQUE,
    search_count BIGINT NOT NULL DEFAULT 0
);