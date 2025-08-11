Project: Book Search API System
Objective
Build a REST API system for book searching based on the given domain and requirements.

Provide API documentation and test environment.

Requirements Summary

Unit and integration tests included

API specification (OpenAPI/Swagger)

Use GitHub Issues for task management

Docker + Docker Compose for deployment

Use appropriate DB (relational or NoSQL)

Domain Modeling
Book Entity:

id (ISBN, string, unique)

title (string)

subtitle (string, optional)

author (string)

publisher (string)

publishedDate (date)

API Endpoints
GET /api/books
Query parameters:

keyword (string) — simple or complex query with OR (|) and NOT (-) operators, max 2 keywords

page (int) — page number, default 1

size (int) — page size, default 20

Response:

searchQuery (string)

pageInfo (currentPage, pageSize, totalPages, totalElements)

books (list of books)

searchMetadata (executionTime in ms, strategy: OR_OPERATION / NOT_OPERATION / SIMPLE)

GET /api/books/{id}

Return full detail of the book with given ISBN

GET /api/search/books

Same as GET /api/books but explicitly for search

GET /api/search/popular

Return top 10 popular search keywords (with counts)

Search Logic
Parse query string to detect operators:

| means OR operation: return books matching any keyword

- means NOT operation: include first keyword, exclude second keyword

Support max 2 keywords

Search target fields: title, subtitle, author

Return paged results

Data
Provide seed script to insert at least 100 sample books with realistic data

Book images can be placeholder URLs

Testing
Unit tests for:

Domain model validation

Search query parsing logic

Service layer book search

Integration tests for API endpoints (mock DB or real DB)

Tests should cover edge cases (empty queries, invalid formats, paging)

Documentation
Provide API spec (OpenAPI/Swagger)

Include example requests/responses matching spec

Instructions to run project with Docker Compose

Git Workflow
Use meaningful commit messages (feat:, fix:, test:, docs:)

Create branches per feature/issue

Use GitHub Issues to track tasks

Optional Features (Extra Credit)
Simple frontend client for API testing (React or vanilla)

Infinite scroll on search results

Caching search results (in-memory or Redis)

Rate limiting middleware

API key authentication

Logging and monitoring setup

How to Run
docker-compose up --build

Seed database automatically on startup

API available on localhost:8080 (or configured port)

Swagger UI at /swagger-ui.html or /api-docs

Deliverables
Source code repository (prefer GitHub)

README with setup instructions

Seed data script

API specification file (OpenAPI yaml/json)

Tests and test reports

Notes for AI Agent Implementation
Prioritize domain correctness and RESTful API design

Implement search parsing as separate module for maintainability

Measure execution time of search and include in response metadata

Ensure pagination metadata is accurate

Use proper HTTP status codes and error messages

Use environment variables for configs (DB connection, port)

Dockerfile and docker-compose.yml should allow one-command startup

Include meaningful logging for debugging

Keep code modular and well-commented

## Project Architecture & Package Structure

### Overview
This project follows Clean Architecture principles with a modular structure. The architecture separates business logic from infrastructure concerns while maintaining clear boundaries between layers.

### Current Package Structure
```
com.trevari/
├── TrevariApplication.java                    # Main Spring Boot application
│
├── book/                                      # Book domain module
│   ├── domain/                               # Domain layer (business logic)
│   │   ├── Book.java                         # Book entity
│   │   ├── BookRepository.java               # Domain repository interface (POJO)
│   │   ├── SearchKeyword.java                # SearchKeyword entity
│   │   └── SearchKeywordRepository.java      # Domain repository interface (POJO)
│   │
│   ├── dto/                                  # Data Transfer Objects
│   │   ├── request/
│   │   │   └── BookSearchRequest.java        # Search request DTO
│   │   └── response/
│   │       ├── BookResponse.java             # Book response DTO
│   │       ├── BookSearchResponse.java       # Search response DTO
│   │       ├── PopularSearchResponse.java    # Popular keywords response DTO
│   │       └── SearchMetadata.java           # Search metadata DTO
│   │
│   ├── exception/                            # Book-specific exceptions
│   │   ├── BookException.java                # Custom book exception
│   │   └── BookExceptionCode.java            # Book error codes enum
│   │
│   └── persistence/                          # Persistence layer (infrastructure)
│       ├── BookJpaRepository.java            # Book JPA repository
│       └── SearchKeywordJpaRepository.java   # SearchKeyword JPA repository
│
└── global/                                   # Global/Common components
    ├── dto/                                  # Global DTOs
    │   ├── ApiResponse.java                  # Common API response wrapper
    │   ├── ErrorDetail.java                 # Error detail information
    │   ├── ErrorResponse.java               # Error response DTO
    │   └── PageInfo.java                    # Pagination information
    │
    └── exception/                           # Global exception handling
        ├── ExceptionCode.java               # Exception code interface
        └── GlobalExceptionHandler.java      # Global exception handler
```

### Architecture Principles

#### 1. Clean Architecture
- **Domain Layer**: Pure business logic without external dependencies
- **Infrastructure Layer**: JPA repositories, external service integrations
- **Application Layer**: Controllers, services (to be implemented)
- **Interface Layer**: DTOs, REST endpoints

#### 2. Domain-Driven Design (DDD)
- **Book Module**: Aggregates all book-related functionality
- **Domain Entities**: `Book`, `SearchKeyword` with business logic
- **Repository Pattern**: Domain interfaces implemented by infrastructure

#### 3. Repository Pattern Implementation
- **Domain Repositories**: POJO interfaces in `book.domain` package
- **JPA Repositories**: Infrastructure implementations extending `JpaRepository` and domain interfaces
- **No Implementation Classes**: Direct inheritance eliminates boilerplate code

#### 4. Global vs Domain-Specific Components
- **Global**: Shared across all modules (`ApiResponse`, `PageInfo`, `GlobalExceptionHandler`)
- **Domain-Specific**: Belongs to specific business context (`BookException`, `BookResponse`)

### Benefits
- **Modularity**: Clear separation between book domain and global concerns
- **Testability**: Domain logic can be tested independently
- **Maintainability**: Changes in one layer don't affect others
- **Scalability**: Easy to add new domain modules (e.g., user, order)
- **Framework Independence**: Domain layer has minimal external dependencies

### Design Patterns Used
- **Repository Pattern**: Data access abstraction
- **DTO Pattern**: Data transfer between layers
- **Exception Handling Pattern**: Centralized error handling
- **Factory Pattern**: Exception code creation