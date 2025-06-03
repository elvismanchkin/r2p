package dev.tsvinc.r2p.api.controller;

import dev.tsvinc.r2p.api.dto.request.AmendR2pRequest;
import dev.tsvinc.r2p.api.dto.request.CancelR2pRequest;
import dev.tsvinc.r2p.api.dto.request.ConfirmR2pRequest;
import dev.tsvinc.r2p.api.dto.request.Creditor;
import dev.tsvinc.r2p.api.dto.request.InitiateR2pRequest;
import dev.tsvinc.r2p.api.dto.request.MessageEvent;
import dev.tsvinc.r2p.api.dto.request.NotificationR2pRequest;
import dev.tsvinc.r2p.api.dto.request.PaymentRequest;
import dev.tsvinc.r2p.api.dto.request.PaymentRequestDetail;
import dev.tsvinc.r2p.api.dto.request.RefundPaymentRequest;
import dev.tsvinc.r2p.api.dto.request.RefundR2pRequest;
import dev.tsvinc.r2p.api.dto.request.ReminderEvent;
import dev.tsvinc.r2p.api.dto.request.RequestOptions;
import dev.tsvinc.r2p.api.dto.request.RequestReason;
import dev.tsvinc.r2p.api.dto.request.SettlementOption;
import dev.tsvinc.r2p.api.dto.request.TaggedTransaction;
import dev.tsvinc.r2p.api.dto.request.TransactionTaggingRequest;
import dev.tsvinc.r2p.api.dto.response.AmendR2pResponse;
import dev.tsvinc.r2p.api.dto.response.CancelR2pResponse;
import dev.tsvinc.r2p.api.dto.response.ConfirmR2pResponse;
import dev.tsvinc.r2p.api.dto.response.InitiateR2pResponse;
import dev.tsvinc.r2p.api.dto.response.PaymentRequestMinResponse;
import dev.tsvinc.r2p.api.dto.response.RefundR2pResponse;
import dev.tsvinc.r2p.domain.enums.AliasType;
import dev.tsvinc.r2p.domain.enums.Product;
import dev.tsvinc.r2p.domain.enums.SettlementSystem;
import dev.tsvinc.r2p.domain.enums.TransactionStatus;
import dev.tsvinc.r2p.domain.enums.UseCase;
import dev.tsvinc.r2p.service.RequestToPayOutboundService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(RequestToPayOutboundController.class)
@Import(RequestToPayOutboundController.class)
@TestPropertySource(properties = {
        "management.prometheus.metrics.export.enabled=false",
        "management.endpoints.web.exposure.include=health,info",
        "spring.main.web-application-type=reactive",
        "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration,org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration,org.springframework.cloud.vault.config.VaultAutoConfiguration",
        "spring.main.allow-bean-definition-overriding=true",
        "spring.cloud.vault.enabled=false",
        "spring.config.import="
})
class RequestToPayOutboundControllerTest {

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
    private RequestToPayOutboundService outboundService;

    @Test
    void confirmR2P_Success() {
        // Given
        String keyId = "test-key-id";
        String paymentRequestId = "PAY123456789";

        ConfirmR2pRequest request = new ConfirmR2pRequest(
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
                paymentRequestId,
                "e2eId1",
                "REQ123456789",
                TransactionStatus.ACSC,
                Instant.now().toString()
        );

        when(outboundService.processConfirmation(eq(paymentRequestId), eq(keyId), eq("test-correlation-id"), any(ConfirmR2pRequest.class)))
                .thenReturn(Mono.just(response));

        // When/Then
        webTestClient.patch()
                .uri("/rtx/api/outbound/v1/requestToPay/{paymentRequestId}/confirm", paymentRequestId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .header("x-correlation-id", "test-correlation-id")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("x-correlation-id")
                .expectBody()
                .jsonPath("$.responseMessageId").isEqualTo("RESP987654321")
                .jsonPath("$.paymentRequestId").isEqualTo(paymentRequestId);
    }

    @Test
    void transactionTagging_Success() {
        // Given
        String keyId = "test-key-id";

        TransactionTaggingRequest request = new TransactionTaggingRequest(
                new MessageEvent("Thank you!", null),
                new TaggedTransaction("PAYREQID12345", "R2P", null, null)
        );

        when(outboundService.processTransactionTagging(eq(keyId), eq("test-correlation-id"), any(TransactionTaggingRequest.class)))
                .thenReturn(Mono.empty());

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/outbound/v1/requestToPay/transaction/tag")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .header("x-correlation-id", "test-correlation-id")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void refundR2P_Success() {
        // Given
        String keyId = "test-key-id";
        String originalPaymentRequestId = "PAY123456789";

        RefundR2pRequest request = new RefundR2pRequest(
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

        when(outboundService.processRefund(eq(originalPaymentRequestId), eq(keyId), eq("test-correlation-id"), any(RefundR2pRequest.class)))
                .thenReturn(Mono.just(response));

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/outbound/v1/requestToPay/{originalPaymentRequestId}/refund", originalPaymentRequestId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .header("x-correlation-id", "test-correlation-id")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("x-correlation-id")
                .expectBody()
                .jsonPath("$.responseMessageId").isEqualTo("RESP987654321")
                .jsonPath("$.requestMessageId").isEqualTo("REQ123456789");
    }

    @Test
    void cancelR2P_Success() {
        // Given
        String keyId = "test-key-id";
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

        when(outboundService.processCancellation(eq(paymentRequestId), eq(keyId), eq("test-correlation-id"), any(CancelR2pRequest.class)))
                .thenReturn(Mono.just(response));

        // When/Then
        webTestClient.patch()
                .uri("/rtx/api/outbound/v1/requestToPay/{paymentRequestId}/cancel", paymentRequestId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .header("x-correlation-id", "test-correlation-id")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("x-correlation-id")
                .expectBody()
                .jsonPath("$.responseMessageId").isEqualTo("RESP987654321")
                .jsonPath("$.requestMessageId").isEqualTo("REQ123456789");
    }

    @Test
    void initiateR2P_Success() {
        // Given
        String keyId = "test-key-id";

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
                new RequestOptions(false, false, null),
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

        when(outboundService.processInitiation(eq(keyId), eq("test-correlation-id"), any(InitiateR2pRequest.class)))
                .thenReturn(Mono.just(response));

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/outbound/v1/requestToPay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .header("x-correlation-id", "test-correlation-id")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("x-correlation-id")
                .expectBody()
                .jsonPath("$.responseMessageId").isEqualTo("RESP987654321")
                .jsonPath("$.requestMessageId").isEqualTo("REQ123456789");
    }

    @Test
    void amendR2P_Success() {
        // Given
        String keyId = "test-key-id";
        String paymentRequestId = "PAY123456789";

        AmendR2pRequest request = new AmendR2pRequest(
                "REQMSGID12345",
                "2024-12-31",
                new RequestReason("Amend reason", null, null, null),
                new PaymentRequest(new BigDecimal("200.00")),
                List.of(new SettlementOption(SettlementSystem.VISA_DIRECT, "4145123412341234", null, null)),
                null,
                "2024-06-01T12:00:00Z"
        );

        AmendR2pResponse response = new AmendR2pResponse(
                "RESP987654321",
                paymentRequestId,
                "REQ123456789",
                TransactionStatus.ACSC,
                Instant.now().toString()
        );

        when(outboundService.processAmendment(eq(paymentRequestId), eq(keyId), eq("test-correlation-id"), any(AmendR2pRequest.class)))
                .thenReturn(Mono.just(response));

        // When/Then
        webTestClient.patch()
                .uri("/rtx/api/outbound/v1/requestToPay/{paymentRequestId}/amend", paymentRequestId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .header("x-correlation-id", "test-correlation-id")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("x-correlation-id")
                .expectBody()
                .jsonPath("$.responseMessageId").isEqualTo("RESP987654321")
                .jsonPath("$.requestMessageId").isEqualTo("REQ123456789");
    }

    @Test
    void notifications_Success() {
        // Given
        String keyId = "test-key-id";

        NotificationR2pRequest request = new NotificationR2pRequest(
                "agentId",
                List.of(new ReminderEvent(
                        "PAY123456789",
                        "REMINDER"
                )),
                "REQMSGID12345",
                "2024-06-01T12:00:00Z"
        );

        when(outboundService.processNotifications(eq(keyId), any(NotificationR2pRequest.class)))
                .thenReturn(Mono.empty());

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/outbound/v1/requestToPay/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .header("x-correlation-id", "test-correlation-id")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void confirmR2P_MissingKeyId() {
        // Given
        String paymentRequestId = "PAY123456789";
        ConfirmR2pRequest request = new ConfirmR2pRequest(
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
                .uri("/rtx/api/outbound/v1/requestToPay/{paymentRequestId}/confirm", paymentRequestId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("x-correlation-id", "test-correlation-id")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void confirmR2P_InvalidContentType() {
        // Given
        String keyId = "test-key-id";
        String paymentRequestId = "PAY123456789";
        String invalidRequest = "invalid request";

        // When/Then
        webTestClient.patch()
                .uri("/rtx/api/outbound/v1/requestToPay/{paymentRequestId}/confirm", paymentRequestId)
                .contentType(MediaType.TEXT_PLAIN)
                .header("keyID", keyId)
                .header("x-correlation-id", "test-correlation-id")
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isEqualTo(415); // 415 Unsupported Media Type
    }

    @Test
    void confirmR2P_ServiceError() {
        // Given
        String keyId = "test-key-id";
        String paymentRequestId = "PAY123456789";

        ConfirmR2pRequest request = new ConfirmR2pRequest(
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

        when(outboundService.processConfirmation(eq(paymentRequestId), eq(keyId), eq("test-correlation-id"), any(ConfirmR2pRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        // When/Then
        webTestClient.patch()
                .uri("/rtx/api/outbound/v1/requestToPay/{paymentRequestId}/confirm", paymentRequestId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .header("x-correlation-id", "test-correlation-id")
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError();
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
                new RequestOptions(false, false, null),
                "2024-06-01T12:00:00Z"
        );

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/outbound/v1/requestToPay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("x-correlation-id", "test-correlation-id")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void initiateR2P_InvalidContentType() {
        // Given
        String keyId = "test-key-id";
        String invalidRequest = "invalid request";

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/outbound/v1/requestToPay")
                .contentType(MediaType.TEXT_PLAIN)
                .header("keyID", keyId)
                .header("x-correlation-id", "test-correlation-id")
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isEqualTo(415); // 415 Unsupported Media Type
    }

    @Test
    void initiateR2P_ServiceError() {
        // Given
        String keyId = "test-key-id";
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
                new RequestOptions(false, false, null),
                "2024-06-01T12:00:00Z"
        );
        when(outboundService.processInitiation(eq(keyId), eq("test-correlation-id"), any(InitiateR2pRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/outbound/v1/requestToPay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .header("x-correlation-id", "test-correlation-id")
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void initiateR2P_ValidationError() {
        // Given
        String keyId = "test-key-id";
        String requestAffinity = "test-affinity";
        // Invalid: paymentRequests is empty
        InitiateR2pRequest request = new InitiateR2pRequest(
                Product.VD,
                UseCase.P2P,
                new RequestReason("Test reason", null, null, null),
                List.of(),
                "2024-12-31",
                "REQMSGID12345",
                List.of(new SettlementOption(SettlementSystem.VISA_DIRECT, "4145123412341234", null, null)),
                new Creditor("agentId", "UA", "UA", null, AliasType.MOBL, "Jane", "D.", List.of()),
                new RequestOptions(false, false, null),
                "2024-06-01T12:00:00Z"
        );

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/outbound/v1/requestToPay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .header("x-correlation-id", "test-correlation-id")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void initiateR2P_NotFoundError() {
        // Given
        String keyId = "test-key-id";
        String requestAffinity = "test-affinity";
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
                new RequestOptions(false, false, null),
                "2024-06-01T12:00:00Z"
        );
        when(outboundService.processInitiation(eq(keyId), eq("test-correlation-id"), any(InitiateR2pRequest.class)))
                .thenReturn(Mono.error(new dev.tsvinc.r2p.exception.R2PNotFoundException("Not found")));

        // When/Then
        webTestClient.post()
                .uri("/rtx/api/outbound/v1/requestToPay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("keyID", keyId)
                .header("x-correlation-id", "test-correlation-id")
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }
} 