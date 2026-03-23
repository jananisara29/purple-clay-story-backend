package com.purpleclay.jewelry;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6370",
    "spring.cache.type=none",
    "jwt.secret=test-secret-key-minimum-256-bits-for-junit-only",
    "openai.api-key=test-key"
})
class JewelryApplicationIntegrationTest {

    @Test
    void contextLoads() {
        // Verifies the full Spring context starts without errors
    }
}
