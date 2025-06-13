package dev.tsvinc.r2p.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;
import org.testcontainers.vault.VaultContainer;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VaultStartupTest {

    private static VaultContainer<?> vaultContainer;
    private static VaultTemplate vaultTemplate;

    @BeforeAll
    static void setUp() throws Exception {
        vaultContainer = new VaultContainer<>("hashicorp/vault:1.19.4")
                .withVaultToken("root")
                .withExposedPorts(8200);
        vaultContainer.start();

        // Use the CLI inside the container to change secret/ to KV v1
        vaultContainer.execInContainer("vault", "secrets", "disable", "secret");
        vaultContainer.execInContainer("vault", "secrets", "enable", "-path=secret", "-version=1", "kv");

        String vaultAddress = "http://" + vaultContainer.getHost() + ":" + vaultContainer.getMappedPort(8200);
        VaultEndpoint endpoint = VaultEndpoint.from(URI.create(vaultAddress));
        vaultTemplate = new VaultTemplate(endpoint, new TokenAuthentication("root"));
    }

    @AfterAll
    static void tearDown() {
        vaultContainer.stop();
    }

    @Test
    void vaultIsAccessible() {
        vaultTemplate.write("secret/test", Map.of("key", "value"));
        VaultResponse response = vaultTemplate.read("secret/test");
        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().get("key")).isEqualTo("value");
    }
} 