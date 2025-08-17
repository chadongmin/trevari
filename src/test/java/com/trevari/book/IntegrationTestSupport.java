package com.trevari.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class IntegrationTestSupport {

//    @Container
//    protected static final MySQLContainer<?> MY_SQL_CONTAINER = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
//            .withDatabaseName("trevari_test")
//            .withUsername("trevari")
//            .withPassword("trevari1234");
//
//    @Container
//    protected static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:6.2.6-alpine"))
//            .withExposedPorts(6379);
//
//    @DynamicPropertySource
//    static void containerProperties(DynamicPropertyRegistry registry) {
//        // MySQL properties
//        registry.add("spring.datasource.url", MY_SQL_CONTAINER::getJdbcUrl);
//        registry.add("spring.datasource.username", MY_SQL_CONTAINER::getUsername);
//        registry.add("spring.datasource.password", MY_SQL_CONTAINER::getPassword);
//        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
//
//        // JPA/Hibernate properties for TestContainers
//        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
//        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
//
//        // Redis properties
//        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
//        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
//    }

//    @Autowired
//    protected ObjectMapper mapper;
//
//    @Autowired
//    protected MockMvc mockMvc;


    protected static final Logger log = LogManager.getLogger(IntegrationTestSupport.class);
    private static final Network network = Network.newNetwork();

    @Container
    protected static MySQLContainer<?> MY_SQL_CONTAINER = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.32"))
            .withNetwork(network)
            .withDatabaseName("trevari")
            .withUsername("trevari")
            .withPassword("trevari1234");
    @Container
    protected static GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7.0.12"))
            .withExposedPorts(6379)
            .withNetworkAliases("redis")
            .withReuse(true)
            .withNetwork(network)
            .withStartupAttempts(5)
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1))
            .withStartupTimeout(Duration.ofSeconds(30));

    static {
        CompletableFuture<Void> mysqlFuture = CompletableFuture.runAsync(MY_SQL_CONTAINER::start);
        CompletableFuture<Void> redisFuture = CompletableFuture.runAsync(REDIS_CONTAINER::start);
        CompletableFuture.allOf(mysqlFuture, redisFuture).join();
    }

    @Autowired
    protected ObjectMapper mapper;
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    private DataInitializer dataInitializer;


    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        // MySQL properties
        registry.add("spring.datasource.url", MY_SQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MY_SQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MY_SQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        
        // JPA/Hibernate properties for TestContainers
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
        
        // Redis properties
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379).toString());
    }

    @AfterEach
    void deleteAll() {
        log.info("데이터 초기화 dataInitializer.deleteAll() 시작");
        dataInitializer.deleteAll();
        log.info("데이터 초기화 dataInitializer.deleteAll() 종료");
    }

}