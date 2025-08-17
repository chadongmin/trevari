package com.trevari.book.presentation;

import com.trevari.book.dto.response.BookSearchResponse;
import com.trevari.book.dto.response.DetailedBookResponse;
import com.trevari.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Book API", description = "도서 조회 및 검색 API")
public interface BookApi {

    @Operation(
            summary = "도서 상세 조회",
            description = "ISBN으로 특정 도서의 상세 정보를 조회합니다. " +
                    "제목, 부제목, 저자, 출판사, 출간일, 카테고리, 이미지 URL 등 " +
                    "모든 도서 정보를 포함한 완전한 상세 데이터를 반환합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "도서 상세 정보 조회 성공 - 모든 도서 필드 포함",
                    content = @Content(schema = @Schema(implementation = DetailedBookResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 ISBN 형식 - 빈 값 또는 유효하지 않은 형식",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "도서를 찾을 수 없음 - 해당 ISBN의 도서가 존재하지 않음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "요청 한도 초과 - 10초 동안 3회 제한",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<DetailedBookResponse>> getBookDetail(
            @Parameter(description = "도서 ISBN (13자리)", required = true, example = "9789355510082")
            @PathVariable String isbn);


    @Operation(
            summary = "전체 도서 목록 조회",
            description = "시스템에 등록된 모든 도서를 페이징하여 조회합니다. " +
                    "검색 조건 없이 전체 도서 목록을 반환하며, 페이징 정보와 함께 제공됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "전체 도서 목록 조회 성공 - 페이징된 도서 목록과 메타데이터 포함",
                    content = @Content(schema = @Schema(implementation = BookSearchResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "요청 한도 초과 - 1분 동안 100회 제한",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<BookSearchResponse>> getAllBooks(
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size);

    @Operation(
            summary = "카테고리별 도서 조회",
            description = "특정 카테고리에 속한 도서들을 조회합니다. " +
                    "카테고리명으로 필터링된 도서 목록을 페이징하여 반환합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "카테고리별 도서 조회 성공 - 해당 카테고리의 도서 목록과 메타데이터 포함",
                    content = @Content(schema = @Schema(implementation = BookSearchResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 카테고리명 - 빈 값 또는 유효하지 않은 카테고리",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "요청 한도 초과 - 1분 동안 100회 제한",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<BookSearchResponse>> getBooksByCategory(
            @Parameter(description = "카테고리명", required = true, example = "Programming")
            @PathVariable String categoryName,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기 (1-100)", example = "20")
            @RequestParam(defaultValue = "20") int size);
}
