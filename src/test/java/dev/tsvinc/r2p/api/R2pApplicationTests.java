package dev.tsvinc.r2p.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@SpringBootTest
@TestPropertySource(properties = {
        "spring.cloud.vault.enabled=false",
        "spring.config.import="
})
class R2pApplicationTests {

    @Test
    void contextLoads() {
    }
}
