# MySQL Testcontainers Configuration

## Overview

This project uses [Testcontainers](https://www.testcontainers.org/) with MySQL for integration testing. This approach allows tests to run against a real MySQL database instead of an in-memory H2 database, providing a more production-like test environment.

## Configuration Details

The MySQL testcontainer is configured in the `IntegrationTestSupport` class with optimized settings to prevent connection timeout issues.

### Key Components

1. **MySQL Container Configuration**:
   ```java
   @Container
   protected static final MySQLContainer<?> MY_SQL_CONTAINER = new MySQLContainer<>("mysql:8.0")
           .withDatabaseName("trevari_test")
           .withUsername("trevari")
           .withPassword("trevari1234")
           .withCommand("--character-set-server=utf8mb4", 
                        "--collation-server=utf8mb4_unicode_ci",
                        "--max_connections=50",
                        "--innodb_buffer_pool_size=5M")
           .withUrlParam("useSSL", "false")
           .withUrlParam("allowPublicKeyRetrieval", "true")
           .withUrlParam("characterEncoding", "UTF-8")
           .withUrlParam("serverTimezone", "UTC")
           .withUrlParam("useUnicode", "true")
           .withUrlParam("connectTimeout", "5000")
           .withUrlParam("socketTimeout", "8000");
   ```

2. **Dynamic Property Configuration**:
   ```java
   @DynamicPropertySource
   static void mysqlProperties(DynamicPropertyRegistry registry) {
       registry.add("spring.datasource.url", MY_SQL_CONTAINER::getJdbcUrl);
       registry.add("spring.datasource.username", MY_SQL_CONTAINER::getUsername);
       registry.add("spring.datasource.password", MY_SQL_CONTAINER::getPassword);
       registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
       
       // HikariCP configuration for tests
       registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
       registry.add("spring.datasource.hikari.minimum-idle", () -> "1");
       registry.add("spring.datasource.hikari.connection-timeout", () -> "10000");
       registry.add("spring.datasource.hikari.idle-timeout", () -> "30000");
       registry.add("spring.datasource.hikari.max-lifetime", () -> "60000");
       registry.add("spring.datasource.hikari.validation-timeout", () -> "5000");
       registry.add("spring.datasource.hikari.leak-detection-threshold", () -> "60000");
       
       // JPA configuration
       registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
       registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
   }
   ```

## Why Use MySQL Testcontainers?

1. **Production-like Environment**: Tests run against the same database type used in production, reducing "works on test but fails in production" issues.

2. **Schema Validation**: Ensures that entity mappings work correctly with MySQL-specific features and data types.

3. **SQL Compatibility**: Tests MySQL-specific SQL queries and functions that might not be supported by H2.

4. **Realistic Performance**: Provides more accurate performance characteristics for database operations.

## Optimizations to Prevent Connection Timeout Issues

The following optimizations have been implemented to prevent connection timeout issues:

1. **MySQL Container Optimizations**:
   - Limited max connections to 50
   - Reduced InnoDB buffer pool size to 5MB
   - Set appropriate character set and collation
   - Added connection timeout parameters

2. **HikariCP Connection Pool Optimizations**:
   - Reduced maximum pool size to 5 (default is 10)
   - Set minimum idle connections to 1
   - Configured appropriate timeouts for connections
   - Added leak detection threshold

3. **JPA Configuration**:
   - Set ddl-auto to create-drop to ensure tables are created automatically
   - Configured the correct MySQL dialect

## Best Practices for Using MySQL Testcontainers

1. **Resource Management**:
   - Keep connection pool sizes small for tests
   - Set appropriate timeouts to prevent hanging tests
   - Use a lightweight MySQL configuration with minimal memory usage

2. **Container Lifecycle**:
   - Use the `@Container` annotation to ensure proper container lifecycle management
   - Consider using a shared container for multiple test classes to improve performance

3. **Test Data Management**:
   - Use `@Transactional` on test classes to automatically roll back changes
   - Consider using test data builders or fixtures for consistent test data

4. **Performance Considerations**:
   - Tests with testcontainers will be slower than with H2
   - Consider using H2 for unit tests and MySQL testcontainers for integration tests
   - Use container reuse between test runs when possible

## Troubleshooting

If you encounter connection timeout issues:

1. **Check Connection Pool Settings**: Ensure the connection pool size is appropriate for your test environment.

2. **Verify Container Health**: Make sure the MySQL container is starting correctly and is healthy.

3. **Increase Timeouts**: If tests are timing out, consider increasing the connection timeout values.

4. **Memory Issues**: If the container is failing due to memory constraints, reduce the MySQL memory settings or increase the available memory for Docker.

5. **Container Logs**: Examine the container logs for any MySQL-specific errors or warnings.