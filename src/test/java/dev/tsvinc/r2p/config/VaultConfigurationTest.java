package dev.tsvinc.r2p.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cloud.vault.authentication=TOKEN",
    "spring.cloud.vault.token=root",
    "spring.cloud.vault.ssl.enabled=false",
    "spring.cloud.vault.uri=http://localhost:${vault.port}"
})
@TestInstance(Lifecycle.PER_CLASS)
class VaultConfigurationTest {

    public static VaultContainer<?> vaultContainer = new VaultContainer<>(DockerImageName.parse("hashicorp/vault:1.19.4"))
            .withVaultToken("root")
            .withExposedPorts(8200);

    private static String roleId;
    private static String secretId;

    static {
        vaultContainer.start();
        System.setProperty("vault.port", String.valueOf(vaultContainer.getMappedPort(8200)));
        try {
            vaultContainer.execInContainer("vault", "secrets", "disable", "secret");
            vaultContainer.execInContainer("vault", "secrets", "enable", "-path=secret", "-version=1", "kv");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeAll
    static void setUpVault() throws Exception {
        // Enable AppRole auth
        vaultContainer.execInContainer("vault", "auth", "enable", "approle");
        // Create policy
        vaultContainer.execInContainer("vault", "policy", "write", "test-policy", "-<<EOF\npath \"secret/*\" { capabilities = [\"create\", \"read\", \"update\", \"delete\", \"list\"] }\nEOF");
        // Create role
        vaultContainer.execInContainer("vault", "write", "auth/approle/role/test-role", "policies=test-policy", "secret_id_ttl=0", "token_ttl=1h", "token_max_ttl=4h");
        // Get role_id
        var roleIdResult = vaultContainer.execInContainer("vault", "read", "-field=role_id", "auth/approle/role/test-role/role-id");
        roleId = roleIdResult.getStdout().trim();
        // Get secret_id
        var secretIdResult = vaultContainer.execInContainer("vault", "write", "-field=secret_id", "auth/approle/role/test-role/secret-id");
        secretId = secretIdResult.getStdout().trim();
        // Write test secret
        vaultContainer.execInContainer("vault", "kv", "put", "secret/myapp/test", "key1=value1", "key2=value2");
    }

    @Autowired
    private VaultTemplate vaultTemplate;

    @BeforeEach
    void setUp() {
        vaultTemplate.write("secret/myapp/test", Map.of(
            "key1", "value1",
            "key2", "value2"
        ));
    }

    @Test
    void vaultTemplateShouldBeInitialized() {
        assertThat(vaultTemplate).isNotNull();
    }

    @Test
    void shouldBeAbleToReadFromVault() {
        VaultResponse response = vaultTemplate.read("secret/myapp/test");
        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().get("key1")).isEqualTo("value1");
        assertThat(response.getData().get("key2")).isEqualTo("value2");
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.vault.uri", () -> "http://" + vaultContainer.getHost() + ":" + vaultContainer.getMappedPort(8200));
    }
} 