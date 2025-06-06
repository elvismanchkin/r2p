package dev.tsvinc.r2p.api.controller;

import dev.tsvinc.r2p.api.dto.request.AmendR2pRequest;
import dev.tsvinc.r2p.api.dto.request.CancelR2pRequest;
import dev.tsvinc.r2p.api.dto.request.ConfirmR2pRequest;
import dev.tsvinc.r2p.api.dto.request.InitiateR2pRequest;
import dev.tsvinc.r2p.api.dto.request.NotificationR2pRequest;
import dev.tsvinc.r2p.api.dto.request.RefundR2pRequest;
import dev.tsvinc.r2p.api.dto.request.TransactionTaggingRequest;
import dev.tsvinc.r2p.api.dto.response.AmendR2pResponse;
import dev.tsvinc.r2p.api.dto.response.CancelR2pResponse;
import dev.tsvinc.r2p.api.dto.response.ConfirmR2pResponse;
import dev.tsvinc.r2p.api.dto.response.ErrorResponse;
import dev.tsvinc.r2p.api.dto.response.InitiateR2pResponse;
import dev.tsvinc.r2p.api.dto.response.RefundR2pResponse;
import dev.tsvinc.r2p.exception.R2PBusinessException;
import dev.tsvinc.r2p.exception.R2PNotFoundException;
import dev.tsvinc.r2p.exception.R2PValidationException;
import dev.tsvinc.r2p.service.RequestToPayOutboundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/rtx/api/outbound/v1/requestToPay")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Request to Pay Outbound", description = "Endpoints for receiving Visa R2P notifications")
public class RequestToPayOutboundController {

    private final RequestToPayOutboundService outboundService;

    @Operation(summary = "Confirm R2P", description = "Receive confirmation notification from Visa about R2P status")
    @ApiResponse(responseCode = "200", description = "Confirmation processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PatchMapping(value = "/{paymentRequestId}/confirm",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ConfirmR2pResponse>> confirmR2P(
            @Parameter(description = "Request to Pay transaction ID", required = true)
            @PathVariable String paymentRequestId,
            @RequestHeader(value = "keyID", required = true) String keyId,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @Valid @RequestBody ConfirmR2pRequest request) {

        return Mono.defer(() -> {
            log.info("Received confirm R2P notification - paymentRequestId: {}, status: {}",
                    paymentRequestId, request.transactionStatus());

            return outboundService.processConfirmation(paymentRequestId, keyId, correlationId, request)
                    .map(response -> ResponseEntity.ok()
                            .header("x-correlation-id", UUID.randomUUID().toString())
                            .body(response))
                    .doOnSuccess(response -> log.info("Confirm R2P processed - paymentRequestId: {}, responseMessageId: {}",
                            paymentRequestId, response.getBody().responseMessageId()))
                    .onErrorResume(error -> handleError("confirm", paymentRequestId, error));
        });
    }

    @Operation(summary = "Transaction Tagging", description = "Receive transaction tagging notification from Visa")
    @ApiResponse(responseCode = "200", description = "Transaction tagging processed successfully")
    @PostMapping(value = "/transaction/tag",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Void>> transactionTagging(
            @RequestHeader(value = "keyID", required = true) String keyId,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @Valid @RequestBody TransactionTaggingRequest request) {

        return Mono.defer(() -> {
            log.info("Received transaction tagging - transactionId: {}, type: {}",
                    request.taggedTransaction().transactionId(),
                    request.taggedTransaction().transactionIdType());

            return outboundService.processTransactionTagging(keyId, correlationId, request)
                    .then(Mono.just(ResponseEntity.ok().<Void>build()))
                    .doOnSuccess(v -> log.info("Transaction tagging processed successfully"))
                    .onErrorResume(error -> handleError("tagging",
                            request.taggedTransaction().transactionId(), error));
        });
    }

    @Operation(summary = "Refund R2P", description = "Receive refund request notification from Visa")
    @ApiResponse(responseCode = "201", description = "Refund request processed successfully")
    @PostMapping(value = "/{originalPaymentRequestId}/refund",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<RefundR2pResponse>> refundR2P(
            @Parameter(description = "Original payment request ID", required = true)
            @PathVariable String originalPaymentRequestId,
            @RequestHeader(value = "keyID", required = true) String keyId,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @Valid @RequestBody RefundR2pRequest request) {

        return Mono.defer(() -> {
            log.info("Received refund R2P notification - originalPaymentRequestId: {}",
                    originalPaymentRequestId);

            return outboundService.processRefund(originalPaymentRequestId, keyId, correlationId, request)
                    .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                            .header("x-correlation-id", UUID.randomUUID().toString())
                            .body(response))
                    .doOnSuccess(response -> log.info("Refund R2P processed - responseMessageId: {}",
                            response.getBody().responseMessageId()))
                    .onErrorResume(error -> handleError("refund", originalPaymentRequestId, error));
        });
    }

    @Operation(summary = "Cancel R2P", description = "Receive cancellation notification from Visa")
    @ApiResponse(responseCode = "200", description = "Cancellation processed successfully")
    @PatchMapping(value = "/{paymentRequestId}/cancel",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<CancelR2pResponse>> cancelR2P(
            @Parameter(description = "Request to Pay transaction ID", required = true)
            @PathVariable String paymentRequestId,
            @RequestHeader(value = "keyID", required = true) String keyId,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @Valid @RequestBody CancelR2pRequest request) {

        return Mono.defer(() -> {
            log.info("Received cancel R2P notification - paymentRequestId: {}, reason: {}",
                    paymentRequestId, request.cancellationReason());

            return outboundService.processCancellation(paymentRequestId, keyId, correlationId, request)
                    .map(response -> ResponseEntity.ok()
                            .header("x-correlation-id", UUID.randomUUID().toString())
                            .body(response))
                    .doOnSuccess(response -> log.info("Cancel R2P processed - responseMessageId: {}",
                            response.getBody().responseMessageId()))
                    .onErrorResume(error -> handleError("cancel", paymentRequestId, error));
        });
    }

    @Operation(summary = "Initiate R2P", description = "Receive new R2P request notification from Visa")
    @ApiResponse(responseCode = "201", description = "R2P request processed successfully")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<InitiateR2pResponse>> initiateR2P(
            @RequestHeader(value = "keyID", required = true) String keyId,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @Valid @RequestBody InitiateR2pRequest request) {

        return Mono.defer(() -> {
            log.info("Received initiate R2P notification - requestMessageId: {}, useCase: {}",
                    request.requestMessageId(), request.useCase());

            return outboundService.processInitiation(keyId, correlationId, request)
                    .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                            .header("x-correlation-id", UUID.randomUUID().toString())
                            .body(response))
                    .doOnSuccess(response -> log.info("Initiate R2P processed - responseMessageId: {}",
                            response.getBody().responseMessageId()))
                    .onErrorResume(error -> handleError("initiate", request.requestMessageId(), error));
        });
    }

    @Operation(summary = "Amend R2P", description = "Receive amendment notification from Visa")
    @ApiResponse(responseCode = "200", description = "Amendment processed successfully")
    @PatchMapping(value = "/{paymentRequestId}/amend",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<AmendR2pResponse>> amendR2P(
            @Parameter(description = "Request to Pay transaction ID", required = true)
            @PathVariable String paymentRequestId,
            @RequestHeader(value = "keyID", required = true) String keyId,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @Valid @RequestBody AmendR2pRequest request) {

        return Mono.defer(() -> {
            log.info("Received amend R2P notification - paymentRequestId: {}", paymentRequestId);

            return outboundService.processAmendment(paymentRequestId, keyId, correlationId, request)
                    .map(response -> ResponseEntity.ok()
                            .header("x-correlation-id", UUID.randomUUID().toString())
                            .body(response))
                    .doOnSuccess(response -> log.info("Amend R2P processed - responseMessageId: {}",
                            response.getBody().responseMessageId()))
                    .onErrorResume(error -> handleError("amend", paymentRequestId, error));
        });
    }

    @Operation(summary = "Notifications R2P", description = "Receive R2P lifecycle event notifications from Visa")
    @ApiResponse(responseCode = "200", description = "Notifications processed successfully")
    @PostMapping(value = "/notifications",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Void>> notifications(
            @RequestHeader(value = "keyID", required = true) String keyId,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @Valid @RequestBody NotificationR2pRequest request) {

        return Mono.defer(() -> {
            log.info("Received notifications - agentId: {}, eventCount: {}",
                    request.agentId(), request.events().size());

            return outboundService.processNotifications(keyId, request)
                    .then(Mono.just(ResponseEntity.ok().<Void>build()))
                    .doOnSuccess(v -> log.info("Notifications processed successfully"))
                    .onErrorResume(error -> handleError("notifications", request.agentId(), error));
        });
    }

    private <T> Mono<ResponseEntity<T>> handleError(String operation, String identifier, Throwable error) {
        log.error("Error processing {} for {}: {}", operation, identifier, error.getMessage(), error);

        ErrorResponse errorResponse = new ErrorResponse(
                error instanceof R2PValidationException || error instanceof R2PBusinessException ? "RC2000" :
                error instanceof R2PNotFoundException ? "RC4000" : "RC5000",
                error instanceof R2PNotFoundException ? error.getMessage() : 
                error instanceof R2PValidationException || error instanceof R2PBusinessException ? error.getMessage() :
                "An unexpected error occurred",
                LocalDateTime.now().toString(),
                identifier,
                UUID.randomUUID().toString(),
                List.of()
        );

        HttpStatus status = error instanceof R2PValidationException || error instanceof R2PBusinessException ? 
                HttpStatus.BAD_REQUEST :
                error instanceof R2PNotFoundException ? HttpStatus.NOT_FOUND :
                HttpStatus.INTERNAL_SERVER_ERROR;

        @SuppressWarnings("unchecked")
        T response = (T) errorResponse;
        return Mono.just(ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response));
    }
}