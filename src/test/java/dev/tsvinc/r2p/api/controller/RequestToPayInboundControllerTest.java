package dev.tsvinc.r2p.api.controller;

import dev.tsvinc.r2p.domain.enums.Product;
import dev.tsvinc.r2p.domain.enums.UseCase;
import dev.tsvinc.r2p.domain.enums.AliasType;
import dev.tsvinc.r2p.domain.enums.SettlementSystem;
import dev.tsvinc.r2p.domain.enums.TransactionStatus;
import dev.tsvinc.r2p.api.dto.request.*;
import dev.tsvinc.r2p.api.dto.response.*;
import dev.tsvinc.r2p.client.dto.request.RetrieveR2pByEndToEndIdsRequest;
import dev.tsvinc.r2p.client.dto.request.RetrieveR2pByPaymentRequestIdsRequest;
import dev.tsvinc.r2p.client.dto.response.MultipleRetrieveR2pResponse;
import dev.tsvinc.r2p.client.dto.response.RetrieveR2pResponse;
import dev.tsvinc.r2p.client.dto.response.PaymentRequestInitiated;
import dev.tsvinc.r2p.client.service.R2PWebClientService;
import dev.tsvinc.r2p.service.validation.R2PBusinessValidationService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(RequestToPayInboundController.class)
@Import(RequestToPayInboundController.class)
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
class RequestToPayInboundControllerTest {

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

    @MockBean
    private R2PBusinessValidationService validationService;

    @Test
    void initiateR2P_Success() {
        // Given
        String keyId = "test-key-id";
        String requestAffinity = "test-affinity";
        String correlationId = "test-correlation-id";

        InitiateR2pRequest request = new InitiateR2pRequest(
                Product.VD,
                UseCase.P2P,
                new RequestReason("Test reason", null, null, null),
                List.of(new PaymentRequestDetail(
                        "e2eId1", "debtorAlias", AliasType.MOBL, "debtorAgentId", "UA", "UA", "John", "D.",
                        new BigDecimal("100.00"), "UAH")),
                "2024-12-31",
                "REQMSGID12345",
                List.of(new SettlementOption(SettlementSystem.VISA_DIRECT, "4145123412341234", null, null)),
                new Creditor("agentId", "UA", "UA", null, AliasType.MOBL, "Jane", "D.", List.of()),
                null,
                "2024-06-01T12:00:00Z"
        );

        InitiateR2pResponse response = new InitiateR2pResponse(
                "RESP987654321",
                List.of(new PaymentRequestMinResponse(
                        "PAY123456789",
                        "e2eId1",
                        TransactionStatus.PDNG,
                        "DEBTOR123",
                        "MOBL"
                )),
                "REQ123456789",
                Instant.now().toString()
        );

        when(webClientService.initiateR2P(eq(keyId), eq(requestAffinity), any(InitiateR2pRequest.class)))
                .thenReturn(Mono.just(response));

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestToPay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("keyID", keyId)
                .header("x-request-affinity", requestAffinity)
                .header("x-correlation-id", correlationId)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("x-request-affinity")
                .expectHeader().exists("x-correlation-id")
                .expectHeader().exists("x-processing-time")
                .expectHeader().exists("Location")
                .expectBody()
                .jsonPath("$.responseMessageId").isEqualTo("RESP987654321")
                .jsonPath("$.requestMessageId").isEqualTo("REQ123456789");
    }

    @Test
    void retrieveR2P_Success() {
        // Given
        String keyId = "test-key-id";
        String requestAffinity = "test-affinity";
        String correlationId = "test-correlation-id";
        String paymentRequestId = "PAY123456789";

        RetrieveR2pResponse response = new RetrieveR2pResponse(
                "product", "useCase", "transactionStatus", "RESP987654321", "2024-06-01T12:00:00Z", "2024-06-01T12:00:00Z",
                null, null, null, null, new PaymentRequestInitiated(
                        "payReqId", "e2eId1", "debtorAgentId", "UA", "UA", "John", "D.", 100.0, "UAH",
                        "debtorAlias", "MOBL", null, null, null, null, null, null, null),
                null, null, List.of(), List.of());

        when(webClientService.retrieveR2P(eq(paymentRequestId), eq(keyId), eq(requestAffinity)))
                .thenReturn(Mono.just(response));

        // When/Then
        webTestClient.get()
                .uri("/rtx/api/v1/requestToPay/{paymentRequestId}", paymentRequestId)
                .header("keyID", keyId)
                .header("x-request-affinity", requestAffinity)
                .header("x-correlation-id", correlationId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("x-request-affinity")
                .expectHeader().exists("x-correlation-id")
                .expectHeader().exists("x-processing-time")
                .expectBody()
                .jsonPath("$.responseMessageId").isEqualTo("RESP987654321");
    }

    @Test
    void retrieveMultipleR2P_ByPaymentRequestIds_Success() {
        // Given
        String keyId = "test-key-id";
        String requestAffinity = "test-affinity";
        String correlationId = "test-correlation-id";

        RetrieveR2pByPaymentRequestIdsRequest request = new RetrieveR2pByPaymentRequestIdsRequest("agentId", List.of("PAY123456789", "PAY987654321"));

        MultipleRetrieveR2pResponse response = new MultipleRetrieveR2pResponse(
                List.of(new RetrieveR2pResponse(
                        "product", "useCase", "ACCP", "RESP987654321", Instant.now().toString(), Instant.now().toString(),
                        null, null, null, null, null, null, null, List.of(), List.of()
                ))
        );

        when(webClientService.retrieveMultipleR2PByPaymentRequestIds(eq(keyId), eq(requestAffinity), any(RetrieveR2pByPaymentRequestIdsRequest.class)))
                .thenReturn(Mono.just(response));

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestToPay/retrieve")
                .contentType(MediaType.APPLICATION_JSON)
                .header("keyID", keyId)
                .header("x-request-affinity", requestAffinity)
                .header("x-correlation-id", correlationId)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("x-request-affinity")
                .expectHeader().exists("x-correlation-id")
                .expectHeader().exists("x-processing-time")
                .expectHeader().exists("X-Total-Count")
                .expectHeader().exists("X-Page-Count")
                .expectHeader().exists("Link")
                .expectBody()
                .jsonPath("$.data.paymentRequestDetails[0].responseMessageId").isEqualTo("RESP987654321")
                .jsonPath("$.metadata").exists()
                .jsonPath("$.pagination").exists();
    }

    @Test
    void confirmR2P_Success() {
        // Given
        String keyId = "test-key-id";
        String requestAffinity = "test-affinity";
        String paymentRequestId = "PAY123456789";

        ConfirmR2pRequest confirmRequest = new ConfirmR2pRequest(
                "PAYREQID12345",
                "e2eId1",
                "REQMSGID12345",
                TransactionStatus.ACSC,
                "AS01",
                "Accepted",
                new BigDecimal("100.00"),
                "UAH",
                null,
                "2024-06-01T12:00:00Z"
        );

        ConfirmR2pResponse response = new ConfirmR2pResponse(
                "RESP987654321",
                "PAY123456789",
                "e2eId1",
                "REQ123456789",
                TransactionStatus.ACSC,
                Instant.now().toString()
        );

        when(webClientService.confirmR2P(eq(paymentRequestId), eq(keyId), eq(requestAffinity), any(ConfirmR2pRequest.class)))
                .thenReturn(Mono.just(response));

        // When/Then
        webTestClient.patch()
                .uri("/rtx/api/v1/requestToPay/{paymentRequestId}/confirm", paymentRequestId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("keyID", keyId)
                .header("x-request-affinity", requestAffinity)
                .bodyValue(confirmRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.responseMessageId").isEqualTo("RESP987654321")
                .jsonPath("$.requestMessageId").isEqualTo("REQ123456789");
    }

    @Test
    void cancelR2P_Success() {
        // Given
        String keyId = "test-key-id";
        String requestAffinity = "test-affinity";
        String paymentRequestId = "PAY123456789";

        CancelR2pRequest request = new CancelR2pRequest(
                "REQMSGID12345",
                "PAID",
                "2024-06-01T12:00:00Z"
        );

        CancelR2pResponse response = new CancelR2pResponse(
                "RESP987654321",
                "REQ123456789",
                TransactionStatus.CNCL,
                Instant.now().toString()
        );

        when(webClientService.cancelR2P(eq(paymentRequestId), eq(keyId), eq(requestAffinity), any(CancelR2pRequest.class)))
                .thenReturn(Mono.just(response));

        // When/Then
        webTestClient.patch()
                .uri("/rtx/api/v1/requestToPay/{paymentRequestId}/cancel", paymentRequestId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("keyID", keyId)
                .header("x-request-affinity", requestAffinity)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.responseMessageId").isEqualTo("RESP987654321")
                .jsonPath("$.requestMessageId").isEqualTo("REQ123456789");
    }

    @Test
    void amendR2P_Success() {
        // Given
        String keyId = "test-key-id";
        String requestAffinity = "test-affinity";
        String paymentRequestId = "PAY123456789";

        AmendR2pRequest amendRequest = new AmendR2pRequest(
                "REQMSGID12345",
                "2024-12-31",
                new RequestReason("Amend reason", null, null, null),
                new PaymentRequest(new BigDecimal("100.00")),
                List.of(new SettlementOption(SettlementSystem.VISA_DIRECT, "4145123412341234", null, null)),
                null,
                "2024-06-01T12:00:00Z"
        );

        AmendR2pResponse response = new AmendR2pResponse(
                "RESP987654321",
                "PAY123456789",
                "REQ123456789",
                TransactionStatus.ACSC,
                Instant.now().toString()
        );

        when(webClientService.amendR2P(eq(paymentRequestId), eq(keyId), eq(requestAffinity), any(AmendR2pRequest.class)))
                .thenReturn(Mono.just(response));

        // When/Then
        webTestClient.patch()
                .uri("/rtx/api/v1/requestToPay/{paymentRequestId}/amend", paymentRequestId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("keyID", keyId)
                .header("x-request-affinity", requestAffinity)
                .bodyValue(amendRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.responseMessageId").isEqualTo("RESP987654321")
                .jsonPath("$.requestMessageId").isEqualTo("REQ123456789");
    }

    @Test
    void refundR2P_Success() {
        // Given
        String keyId = "test-key-id";
        String requestAffinity = "test-affinity";
        String originalPaymentRequestId = "PAY123456789";

        RefundR2pRequest refundRequest = new RefundR2pRequest(
                List.of(new RefundPaymentRequest("e2eId1", new BigDecimal("100.00"))),
                "REQMSGID12345",
                List.of(new SettlementOption(SettlementSystem.VISA_DIRECT, "4145123412341234", null, null)),
                new Creditor("agentId", "UA", "UA", null, AliasType.MOBL, "Jane", "D.", List.of()),
                "2024-06-01T12:00:00Z"
        );

        RefundR2pResponse response = new RefundR2pResponse(
                "RESP987654321",
                List.of(new PaymentRequestMinResponse(
                        originalPaymentRequestId,
                        "e2eId1",
                        TransactionStatus.PDNG,
                        "DEBTOR123",
                        "MOBL"
                )),
                "REQ123456789",
                Instant.now().toString()
        );

        when(webClientService.refundR2P(eq(originalPaymentRequestId), eq(keyId), eq(requestAffinity), any(RefundR2pRequest.class)))
                .thenReturn(Mono.just(response));

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestToPay/{originalPaymentRequestId}/refund", originalPaymentRequestId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("keyID", keyId)
                .header("x-request-affinity", requestAffinity)
                .bodyValue(refundRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.responseMessageId").isEqualTo("RESP987654321")
                .jsonPath("$.requestMessageId").isEqualTo("REQ123456789");
    }

    @Test
    void transactionTagging_Success() {
        // Given
        String keyId = "test-key-id";
        String requestAffinity = "test-affinity";

        TransactionTaggingRequest taggingRequest = new TransactionTaggingRequest(
                new MessageEvent("Thank you!", null),
                new TaggedTransaction("PAYREQID12345", "R2P", null, null)
        );

        when(webClientService.submitTransactionTagging(eq(keyId), eq(requestAffinity), any(TransactionTaggingRequest.class)))
                .thenReturn(Mono.empty());

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestToPay/transaction/tag")
                .contentType(MediaType.APPLICATION_JSON)
                .header("keyID", keyId)
                .header("x-request-affinity", requestAffinity)
                .bodyValue(taggingRequest)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void initiateR2P_ValidationError() {
        // Given
        String keyId = "test-key-id";
        InitiateR2pRequest request = new InitiateR2pRequest(
            null, // Invalid Product
            null, // Invalid UseCase
            null, // Invalid RequestReason
            null, // Invalid PaymentRequestDetail list
            null, // Invalid dueDate
            null, // Invalid requestMessageId
            null, // Invalid SettlementOption list
            null, // Invalid Creditor
            null, // Invalid RequestOptions
            null  // Invalid creationDateTime
        );
        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestToPay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("keyID", keyId)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void initiateR2P_ServiceError() {
        // Given
        String keyId = "test-key-id";
        InitiateR2pRequest request = new InitiateR2pRequest(
                null, // Invalid Product
                UseCase.P2P,
                new RequestReason("Test reason", null, null, null),
                List.of(new PaymentRequestDetail(
                        "e2eId1", "debtorAlias", AliasType.MOBL, "debtorAgentId", "UA", "UA", "John", "D.",
                        new BigDecimal("100.00"), "UAH")),
                "2024-12-31",
                "REQ123456789",
                List.of(new SettlementOption(SettlementSystem.VISA_DIRECT, "4145123412341234", null, null)),
                new Creditor("agentId", "UA", "UA", null, AliasType.MOBL, "Jane", "D.", List.of()),
                null,
                Instant.now().toString()
        );

        when(webClientService.initiateR2P(eq(keyId), any(), any(InitiateR2pRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestToPay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("keyID", keyId)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void retrieveR2P_NotFound() {
        // Given
        String keyId = "test-key-id";
        String paymentRequestId = "NONEXISTENT";

        when(webClientService.retrieveR2P(eq(paymentRequestId), eq(keyId), any()))
                .thenReturn(Mono.error(new RuntimeException("Not found")));

        // When/Then
        webTestClient.get()
                .uri("/rtx/api/v1/requestToPay/{paymentRequestId}", paymentRequestId)
                .header("keyID", keyId)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void retrieveMultipleR2P_InvalidRequest() {
        // Given
        String keyId = "test-key-id";
        Object invalidRequest = Map.of("invalid", "request");

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestToPay/retrieve")
                .contentType(MediaType.APPLICATION_JSON)
                .header("keyID", keyId)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void initiateR2P_MissingKeyId() {
        // Given
        InitiateR2pRequest request = new InitiateR2pRequest(
                Product.VD,
                UseCase.P2P,
                new RequestReason("Test reason", null, null, null),
                List.of(new PaymentRequestDetail(
                        "e2eId1", "debtorAlias", AliasType.MOBL, "debtorAgentId", "UA", "UA", "John", "D.",
                        new BigDecimal("100.00"), "UAH")),
                "2024-12-31",
                "REQMSGID12345",
                List.of(new SettlementOption(SettlementSystem.VISA_DIRECT, "4145123412341234", null, null)),
                new Creditor("agentId", "UA", "UA", null, AliasType.MOBL, "Jane", "D.", List.of()),
                null,
                Instant.now().toString()
        );

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestToPay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-request-affinity", "test-affinity")
                .header("x-correlation-id", "test-correlation-id")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void initiateR2P_InvalidContentType() {
        // Given
        String invalidRequest = "invalid request";

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestToPay")
                .contentType(MediaType.TEXT_PLAIN)
                .header("keyID", "test-key-id")
                .header("x-request-affinity", "test-affinity")
                .header("x-correlation-id", "test-correlation-id")
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isEqualTo(415); // 415 Unsupported Media Type
    }

    @Test
    void retrieveR2P_MissingKeyId() {
        // Given
        String paymentRequestId = "PAY123456789";

        // When/Then
        webTestClient.get()
                .uri("/rtx/api/v1/requestToPay/{paymentRequestId}", paymentRequestId)
                .header("x-request-affinity", "test-affinity")
                .header("x-correlation-id", "test-correlation-id")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void retrieveMultipleR2P_MissingKeyId() {
        // Given
        RetrieveR2pByPaymentRequestIdsRequest request = new RetrieveR2pByPaymentRequestIdsRequest(
                "agentId",
                List.of("PAY123456789", "PAY987654321")
        );

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestToPay/retrieve?page=0&size=10")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-request-affinity", "test-affinity")
                .header("x-correlation-id", "test-correlation-id")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void confirmR2P_MissingKeyId() {
        // Given
        String paymentRequestId = "PAY123456789";
        ConfirmR2pRequest confirmRequest = new ConfirmR2pRequest(
                "PAYREQID12345",
                "e2eId1",
                "REQMSGID12345",
                TransactionStatus.ACSC,
                "AS01",
                "Accepted",
                new BigDecimal("100.00"),
                "UAH",
                null,
                "2024-06-01T12:00:00Z"
        );

        // When/Then
        webTestClient.patch()
                .uri("/rtx/api/v1/requestToPay/{paymentRequestId}/confirm", paymentRequestId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-request-affinity", "test-affinity")
                .bodyValue(confirmRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void cancelR2P_MissingKeyId() {
        // Given
        String paymentRequestId = "PAY123456789";
        CancelR2pRequest cancelRequest = new CancelR2pRequest(
            "REQMSGID12345",
            "PAID",
            "2024-06-01T12:00:00Z"
        );

        // When/Then
        webTestClient.patch()
            .uri("/rtx/api/v1/requestToPay/{paymentRequestId}/cancel", paymentRequestId)
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-request-affinity", "test-affinity")
            .bodyValue(cancelRequest)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void amendR2P_MissingKeyId() {
        // Given
        String paymentRequestId = "PAY123456789";
        AmendR2pRequest amendRequest = new AmendR2pRequest(
            "REQMSGID12345",
            "2024-12-31",
            new RequestReason("Amend reason", null, null, null),
            new PaymentRequest(new BigDecimal("200.00")),
            List.of(new SettlementOption(SettlementSystem.VISA_DIRECT, "4145123412341234", null, null)),
            null,
            "2024-06-01T12:00:00Z"
        );

        // When/Then
        webTestClient.patch()
            .uri("/rtx/api/v1/requestToPay/{paymentRequestId}/amend", paymentRequestId)
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-request-affinity", "test-affinity")
            .bodyValue(amendRequest)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void refundR2P_MissingKeyId() {
        // Given
        String paymentRequestId = "PAY123456789";
        RefundR2pRequest refundRequest = new RefundR2pRequest(
            List.of(new RefundPaymentRequest("e2eId1", new BigDecimal("100.00"))),
            "REQMSGID12345",
            List.of(new SettlementOption(SettlementSystem.VISA_DIRECT, "4145123412341234", null, null)),
            new Creditor("agentId", "UA", "UA", null, AliasType.MOBL, "Jane", "D.", List.of()),
            "2024-06-01T12:00:00Z"
        );

        // When/Then
        webTestClient.patch()
            .uri("/rtx/api/v1/requestToPay/{paymentRequestId}/refund", paymentRequestId)
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-request-affinity", "test-affinity")
            .bodyValue(refundRequest)
            .exchange()
            .expectStatus().isEqualTo(405);
    }

    @Test
    void transactionTagging_MissingKeyId() {
        // Given
        String paymentRequestId = "PAY123456789";
        TransactionTaggingRequest taggingRequest = new TransactionTaggingRequest(
            new MessageEvent("Transaction tagged", null),
            new TaggedTransaction(paymentRequestId, "R2P", null, null)
        );

        // When/Then
        webTestClient.patch()
            .uri("/rtx/api/v1/requestToPay/{paymentRequestId}/tag", paymentRequestId)
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-request-affinity", "test-affinity")
            .bodyValue(taggingRequest)
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void retrieveR2P_InvalidPaymentRequestId() {
        // Given
        String keyId = "test-key-id";
        String requestAffinity = "test-affinity";
        String correlationId = "test-correlation-id";
        String invalidPaymentRequestId = "invalid-id";

        when(webClientService.retrieveR2P(eq(invalidPaymentRequestId), eq(keyId), eq(requestAffinity)))
                .thenReturn(Mono.error(new RuntimeException("Invalid payment request ID")));

        // When/Then
        webTestClient.get()
                .uri("/rtx/api/v1/requestToPay/{paymentRequestId}", invalidPaymentRequestId)
                .header("keyID", keyId)
                .header("x-request-affinity", requestAffinity)
                .header("x-correlation-id", correlationId)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void retrieveMultipleR2P_EmptyList() {
        // Given
        String keyId = "test-key-id";
        String requestAffinity = "test-affinity";
        String correlationId = "test-correlation-id";

        RetrieveR2pByPaymentRequestIdsRequest request = new RetrieveR2pByPaymentRequestIdsRequest(
                "agentId",
                List.of()
        );

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/v1/requestToPay/retrieve")
                .contentType(MediaType.APPLICATION_JSON)
                .header("keyID", keyId)
                .header("x-request-affinity", requestAffinity)
                .header("x-correlation-id", correlationId)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }
} 