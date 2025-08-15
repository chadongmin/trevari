-- Create database tables
CREATE TABLE IF NOT EXISTS book (
    isbn VARCHAR(255) NOT NULL PRIMARY KEY,
    title VARCHAR(1000) NOT NULL,
    subtitle VARCHAR(1000),
    description TEXT,
    page_count INT,
    format VARCHAR(20),
    amount INT,
    currency VARCHAR(10),
    publisher VARCHAR(500) NOT NULL,
    published_date DATE NOT NULL,
    image_url VARCHAR(1000)
);



-- Author 엔티티 테이블
CREATE TABLE IF NOT EXISTS author (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Category 엔티티 테이블
CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- BookAuthor 연결 엔티티 테이블
CREATE TABLE IF NOT EXISTS book_author (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_isbn VARCHAR(255) NOT NULL,
    author_id BIGINT NOT NULL,
    role VARCHAR(100),
    CONSTRAINT fk_book_author_book_isbn 
        FOREIGN KEY (book_isbn) REFERENCES book (isbn) ON DELETE CASCADE,
    CONSTRAINT fk_book_author_author_id 
        FOREIGN KEY (author_id) REFERENCES author (id) ON DELETE CASCADE
);

-- Book-Category 다대다 관계 테이블
CREATE TABLE IF NOT EXISTS book_category (
    book_isbn VARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (book_isbn, category_id),
    CONSTRAINT fk_book_category_book_isbn 
        FOREIGN KEY (book_isbn) REFERENCES book (isbn) ON DELETE CASCADE,
    CONSTRAINT fk_book_category_category_id 
        FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS search_keywords (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(255) NOT NULL UNIQUE,
    search_count BIGINT NOT NULL DEFAULT 0
);