package com.trevari.book.persistence;

import com.trevari.book.domain.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("BookJpaRepository 데이터 액세스 테스트")
class BookJpaRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private BookJpaRepository bookRepository;
    
    private Book javaBook;
    private Book springBook;
    private Book pythonBook;
    
    @BeforeEach
    void setUp() {
        // Given: 테스트 데이터 준비
        javaBook = Book.builder()
                .isbn("9781617297397")
                .title("Java in Action")
                .subtitle("Lambdas, streams, functional and reactive programming")
                .authors(List.of("Raoul-Gabriel Urma", "Mario Fusco"))
                .publisher("Manning Publications")
                .publishedDate(LocalDate.of(2020, 1, 1))
                .build();
        
        springBook = Book.builder()
                .isbn("9781617294945")
                .title("Spring in Action")
                .subtitle("Fifth Edition")
                .authors(List.of("Craig Walls"))
                .publisher("Manning Publications")
                .publishedDate(LocalDate.of(2020, 2, 1))
                .build();
        
        pythonBook = Book.builder()
                .isbn("9781491950401")
                .title("Learning Python")
                .subtitle("Powerful Object-Oriented Programming")
                .authors(List.of("Mark Lutz"))
                .publisher("O'Reilly Media")
                .publishedDate(LocalDate.of(2020, 3, 1))
                .build();
        
        // 데이터 저장
        entityManager.persistAndFlush(javaBook);
        entityManager.persistAndFlush(springBook);
        entityManager.persistAndFlush(pythonBook);
        entityManager.clear();
    }
    
    @Test
    @DisplayName("ISBN으로 도서 조회 성공")
    void findByIsbn_Success() {
        // When
        Optional<Book> result = bookRepository.findByIsbn("9781617297397");
        
        // Then
        assertThat(result).isPresent();
        Book foundBook = result.get();
        assertThat(foundBook.getIsbn()).isEqualTo("9781617297397");
        assertThat(foundBook.getTitle()).isEqualTo("Java in Action");
        assertThat(foundBook.getAuthors()).containsExactly("Raoul-Gabriel Urma", "Mario Fusco");
    }
    
    @Test
    @DisplayName("존재하지 않는 ISBN으로 조회시 빈 결과 반환")
    void findByIsbn_NotFound() {
        // When
        Optional<Book> result = bookRepository.findByIsbn("nonexistent-isbn");
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("키워드로 도서 검색 - 제목에서 검색")
    void findByKeyword_SearchInTitle() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Book> result = bookRepository.findByKeyword("Java", pageable);
        
        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Java in Action");
    }
    
    @Test
    @DisplayName("키워드로 도서 검색 - 부제목에서 검색")
    void findByKeyword_SearchInSubtitle() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Book> result = bookRepository.findByKeyword("functional", pageable);
        
        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSubtitle()).contains("functional");
    }
    
    @Test
    @DisplayName("키워드로 도서 검색 - 저자에서 검색")
    void findByKeyword_SearchInAuthor() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Book> result = bookRepository.findByKeyword("Craig", pageable);
        
        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAuthors()).contains("Craig Walls");
    }
    
    @Test
    @DisplayName("키워드로 도서 검색 - 대소문자 무관")
    void findByKeyword_CaseInsensitive() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Book> result = bookRepository.findByKeyword("JAVA", pageable);
        
        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Java in Action");
    }
    
    @Test
    @DisplayName("OR 검색 - 두 키워드 중 하나라도 매치되면 반환")
    void findByOrKeywords_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Book> result = bookRepository.findByOrKeywords("Java", "Python", pageable);
        
        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Book::getTitle)
                .containsExactlyInAnyOrder("Java in Action", "Learning Python");
    }
    
    @Test
    @DisplayName("NOT 검색 - 첫 번째 키워드는 포함, 두 번째 키워드는 제외")
    void findByNotKeywords_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        
        // When - "Action" 키워드가 있지만 "Java"는 없는 도서
        Page<Book> result = bookRepository.findByNotKeywords("Action", "Java", pageable);
        
        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Spring in Action");
    }
    
    @Test
    @DisplayName("검색 결과 없음")
    void findByKeyword_NoResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Book> result = bookRepository.findByKeyword("NonexistentKeyword", pageable);
        
        // Then
        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("페이징 처리 확인")
    void findByKeyword_Pagination() {
        // Given
        Pageable firstPage = PageRequest.of(0, 2);
        Pageable secondPage = PageRequest.of(1, 2);
        
        // When
        Page<Book> firstResult = bookRepository.findByKeyword("in", firstPage);
        Page<Book> secondResult = bookRepository.findByKeyword("in", secondPage);
        
        // Then
        assertThat(firstResult.getContent()).hasSize(2);
        assertThat(secondResult.getContent()).hasSize(1); // 마지막 페이지라서 1개
        assertThat(firstResult.getTotalElements()).isEqualTo(3); // "Java in Action", "Spring in Action", "Learning Python"
    }
}