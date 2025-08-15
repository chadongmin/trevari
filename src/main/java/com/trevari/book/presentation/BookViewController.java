package com.trevari.book.presentation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 도서 관련 뷰를 제공하는 컨트롤러
 */
@Slf4j
@Controller
public class BookViewController {

    /**
     * 도서 검색 페이지
     */
    @GetMapping("/search")
    public String searchPage(@RequestParam(required = false) String keyword, Model model) {
        log.info("Request to book search page");
        if (keyword != null) {
            model.addAttribute("searchQuery", keyword);
        }
        return "books/search";
    }

    /**
     * 도서 상세 페이지
     *
     * @param isbn 도서 ISBN
     * @param model 모델 객체
     * @return 도서 상세 템플릿
     */
    @GetMapping("/books/{isbn}")
    public String bookDetailPage(@PathVariable String isbn, Model model) {
        log.info("Request to book detail page - ISBN: {}", isbn);
        model.addAttribute("isbn", isbn);
        return "books/detail";
    }
}