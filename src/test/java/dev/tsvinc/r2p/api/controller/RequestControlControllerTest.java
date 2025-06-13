package dev.tsvinc.r2p.api.controller;

import dev.tsvinc.r2p.client.dto.request.ViewBlockRequest;
import dev.tsvinc.r2p.client.dto.response.BlockedPayeeInfo;
import dev.tsvinc.r2p.client.dto.response.ViewBlockResponse;
import dev.tsvinc.r2p.client.service.R2PWebClientService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(RequestControlController.class)
@Import(RequestControlController.class)
@TestPropertySource(properties = {
    "management.prometheus.metrics.export.enabled=false",
    "management.endpoints.web.exposure.include=health,info",
    "spring.main.web-application-type=reactive",
    "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration,org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration",
    "spring.main.allow-bean-definition-overriding=true",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "spring.cloud.vault.enabled=false"
})
class RequestControlControllerTest {

    @Configuration
    static class TestConfig {
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private R2PWebClientService webClientService;

    @Test
    void viewBlocks_Success() {
        // Given
        String keyId = "test-key-id";
        ViewBlockRequest request = new ViewBlockRequest(
                "DEBTOR123",
                "VISA",
                "AGENT123",
                "REQ123456789",
                Instant.now().toString()
        );

        ViewBlockResponse response = new ViewBlockResponse(
                List.of(new BlockedPayeeInfo(
                        "P2P",
                        "Blocked for fraud",
                        "BLOCK123",
                        Instant.now().toString(),
                        "AGENT123",
                        "PAYEE1",
                        "VISA",
                        "John",
                        "Doe"
                )),
                "REQ123456789",
                "RESP987654321",
                Instant.now().toString()
        );

        when(webClientService.viewBlocks(eq(keyId), any(ViewBlockRequest.class)))
                .thenReturn(Mono.just(response));

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestControl/view")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.blockedPayees[0].product").isEqualTo("P2P")
                .jsonPath("$.blockedPayees[0].blockReason").isEqualTo("Blocked for fraud")
                .jsonPath("$.blockedPayees[0].referenceId").isEqualTo("BLOCK123")
                .jsonPath("$.requestMessageId").isEqualTo("REQ123456789")
                .jsonPath("$.responseMessageId").isEqualTo("RESP987654321");
    }

    @Test
    void viewBlocks_EmptyResponse() {
        // Given
        String keyId = "test-key-id";
        ViewBlockRequest request = new ViewBlockRequest(
                "DEBTOR123",
                "VISA",
                "AGENT123",
                "REQ123456789",
                Instant.now().toString()
        );

        ViewBlockResponse response = new ViewBlockResponse(
                List.of(),
                "REQ123456789",
                "RESP987654321",
                Instant.now().toString()
        );

        when(webClientService.viewBlocks(eq(keyId), any(ViewBlockRequest.class)))
                .thenReturn(Mono.just(response));

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestControl/view")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.blockedPayees").isArray()
                .jsonPath("$.blockedPayees").isEmpty()
                .jsonPath("$.requestMessageId").isEqualTo("REQ123456789")
                .jsonPath("$.responseMessageId").isEqualTo("RESP987654321");
    }

    @Test
    void viewBlocks_MissingKeyId() {
        // Given
        ViewBlockRequest request = new ViewBlockRequest(
                "DEBTOR123",
                "VISA",
                "AGENT123",
                "REQ123456789",
                Instant.now().toString()
        );

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestControl/view")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void viewBlocks_InvalidContentType() {
        // Given
        String keyId = "test-key-id";
        String plainTextBody = "This is plain text";

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestControl/view")
                .contentType(MediaType.TEXT_PLAIN)
                .header("Content-Type", MediaType.TEXT_PLAIN_VALUE)
                .header("keyID", keyId)
                .bodyValue(plainTextBody)
                .exchange()
                .expectStatus().isEqualTo(415); // 415 Unsupported Media Type
    }

    @Test
    void viewBlocks_InvalidRequest() {
        // Given
        String keyId = "test-key-id";
        ViewBlockRequest request = new ViewBlockRequest(
                null, // Invalid debtor ID
                "VISA",
                "AGENT123",
                "REQ123456789",
                Instant.now().toString()
        );

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestControl/view")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void viewBlocks_ServiceError() {
        // Given
        String keyId = "test-key-id";
        ViewBlockRequest request = new ViewBlockRequest(
                "DEBTOR123",
                "VISA",
                "AGENT123",
                "REQ123456789",
                Instant.now().toString()
        );

        when(webClientService.viewBlocks(eq(keyId), any(ViewBlockRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestControl/view")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void removeBlock_Success() {
        // Given
        String keyId = "test-key-id";
        String blockReferenceId = "BLOCK123";

        when(webClientService.removeBlock(eq(blockReferenceId), eq(keyId)))
                .thenReturn(Mono.empty());

        // When/Then
        webTestClient.patch()
                .uri("/rtx/api/v1/requestControl/{blockReferenceId}/remove", blockReferenceId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void removeBlock_MissingKeyId() {
        // Given
        String blockReferenceId = "BLOCK123";

        // When/Then
        webTestClient.patch()
                .uri("/rtx/api/v1/requestControl/{blockReferenceId}/remove", blockReferenceId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void removeBlock_InvalidBlockReferenceId() {
        // Given
        String keyId = "test-key-id";
        String blockReferenceId = ""; // Invalid block reference ID

        // When/Then
        webTestClient.patch()
                .uri("/rtx/api/v1/requestControl/{blockReferenceId}/remove", blockReferenceId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void removeBlock_ServiceError() {
        // Given
        String keyId = "test-key-id";
        String blockReferenceId = "BLOCK123";

        when(webClientService.removeBlock(eq(blockReferenceId), eq(keyId)))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        // When/Then
        webTestClient.patch()
                .uri("/rtx/api/v1/requestControl/{blockReferenceId}/remove", blockReferenceId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .exchange()
                .expectStatus().is5xxServerError();
    }
} 