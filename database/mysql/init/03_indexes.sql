-- 검색 성능 최적화를 위한 인덱스 생성

-- 1. 도서 제목 검색 최적화 (길이 제한)
CREATE INDEX idx_book_title ON book(title(255));
CREATE INDEX idx_book_subtitle ON book(subtitle(255));

-- 2. 저자명 검색 최적화
CREATE INDEX idx_author_name ON author(name);

-- 3. 복합 인덱스 (book_author 조인 성능 향상)
CREATE INDEX idx_book_author_isbn ON book_author(book_isbn);
CREATE INDEX idx_book_author_author_id ON book_author(author_id);

-- 4. 카테고리 검색 최적화
CREATE INDEX idx_category_name ON category(name);
CREATE INDEX idx_book_category_isbn ON book_category(book_isbn);
CREATE INDEX idx_book_category_category_id ON book_category(category_id);

-- 5. 풀텍스트 인덱스 (MySQL 5.7+) - 성능 최적화용
-- 단일 컬럼 풀텍스트 인덱스 (안전한 버전)
ALTER TABLE book ADD FULLTEXT INDEX ft_book_title (title);
ALTER TABLE author ADD FULLTEXT INDEX ft_author_name (name);

-- 6. 검색 키워드 최적화
CREATE INDEX idx_search_keywords_keyword ON search_keywords(keyword);
CREATE INDEX idx_search_keywords_count ON search_keywords(search_count DESC);