package dev.tsvinc.r2p.api.controller;

import dev.tsvinc.r2p.client.dto.request.ReferenceDataRequest;
import dev.tsvinc.r2p.client.dto.response.ReferenceDataResponse;
import dev.tsvinc.r2p.client.dto.response.ParticipantInfo;
import dev.tsvinc.r2p.client.service.R2PWebClientService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
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

@WebFluxTest(ReferenceDataController.class)
@Import(ReferenceDataController.class)
@TestPropertySource(properties = {
    "management.metrics.export.prometheus.enabled=false",
    "management.endpoints.web.exposure.include=health,info",
    "spring.main.web-application-type=reactive",
    "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration,org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration",
    "spring.main.allow-bean-definition-overriding=true",
    "spring.redis.host=localhost",
    "spring.redis.port=6379"
})
class ReferenceDataControllerTest {

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
    void getReferenceData_Success() {
        // Given
        String keyId = "test-key-id";
        ReferenceDataRequest request = new ReferenceDataRequest(
                List.of("PARTICIPANTS"),
                "REQ123456789",
                Instant.now().toString()
        );

        ReferenceDataResponse response = new ReferenceDataResponse(
                List.of(new ParticipantInfo("BANK1", "Bank One", List.of("PAYMENT", "REFUND"))),
                "REQ123456789",
                "RESP987654321",
                Instant.now().toString()
        );

        when(webClientService.getReferenceData(eq(keyId), any(ReferenceDataRequest.class)))
                .thenReturn(Mono.just(response));

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestToPay/referenceData")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.availableParticipants[0].participantId").isEqualTo("BANK1")
                .jsonPath("$.availableParticipants[0].participantName").isEqualTo("Bank One")
                .jsonPath("$.availableParticipants[0].supportedFeatures[0]").isEqualTo("PAYMENT")
                .jsonPath("$.availableParticipants[0].supportedFeatures[1]").isEqualTo("REFUND")
                .jsonPath("$.requestMessageId").isEqualTo("REQ123456789")
                .jsonPath("$.responseMessageId").isEqualTo("RESP987654321");
    }

    @Test
    void getReferenceData_InvalidRequest() {
        // Given
        String keyId = "test-key-id";
        ReferenceDataRequest request = new ReferenceDataRequest(
                List.of(), // Empty list should trigger validation error
                "REQ123456789",
                Instant.now().toString()
        );

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestToPay/referenceData")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getReferenceData_ServiceError() {
        // Given
        String keyId = "test-key-id";
        ReferenceDataRequest request = new ReferenceDataRequest(
                List.of("PARTICIPANTS"),
                "REQ123456789",
                Instant.now().toString()
        );

        when(webClientService.getReferenceData(eq(keyId), any(ReferenceDataRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestToPay/referenceData")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError();
    }
} 