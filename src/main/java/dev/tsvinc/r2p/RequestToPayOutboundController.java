package dev.tsvinc.r2p;

import com.company.visa.dto.outbound.*;
import com.company.visa.service.outbound.RequestToPayOutboundService;
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
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
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
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("keyID") String keyId,
            @RequestHeader("x-request-affinity") String requestAffinity,
            @Valid @RequestBody ConfirmR2pRequest request) {

        return Mono.defer(() -> {
            log.info("Received confirm R2P notification - paymentRequestId: {}, status: {}",
                    paymentRequestId, request.transactionStatus());

            return outboundService.processConfirmation(paymentRequestId, keyId, requestAffinity, request)
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
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("keyID") String keyId,
            @RequestHeader(value = "x-request-affinity", required = false) String requestAffinity,
            @Valid @RequestBody TransactionTaggingRequest request) {

        return Mono.defer(() -> {
            log.info("Received transaction tagging - transactionId: {}, type: {}",
                    request.taggedTransaction().transactionId(),
                    request.taggedTransaction().transactionIdType());

            return outboundService.processTransactionTagging(keyId, requestAffinity, request)
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
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("keyID") String keyId,
            @RequestHeader("x-request-affinity") String requestAffinity,
            @Valid @RequestBody RefundR2pRequest request) {

        return Mono.defer(() -> {
            log.info("Received refund R2P notification - originalPaymentRequestId: {}",
                    originalPaymentRequestId);

            return outboundService.processRefund(originalPaymentRequestId, keyId, requestAffinity, request)
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
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("keyID") String keyId,
            @RequestHeader("x-request-affinity") String requestAffinity,
            @Valid @RequestBody CancelR2pRequest request) {

        return Mono.defer(() -> {
            log.info("Received cancel R2P notification - paymentRequestId: {}, reason: {}",
                    paymentRequestId, request.cancellationReason());

            return outboundService.processCancellation(paymentRequestId, keyId, requestAffinity, request)
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
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("keyID") String keyId,
            @RequestHeader("x-request-affinity") String requestAffinity,
            @Valid @RequestBody InitiateR2pRequest request) {

        return Mono.defer(() -> {
            log.info("Received initiate R2P notification - requestMessageId: {}, useCase: {}",
                    request.requestMessageId(), request.useCase());

            return outboundService.processInitiation(keyId, requestAffinity, request)
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
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("keyID") String keyId,
            @RequestHeader("x-request-affinity") String requestAffinity,
            @Valid @RequestBody AmendR2pRequest request) {

        return Mono.defer(() -> {
            log.info("Received amend R2P notification - paymentRequestId: {}", paymentRequestId);

            return outboundService.processAmendment(paymentRequestId, keyId, requestAffinity, request)
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
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("keyID") String keyId,
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

        if (error instanceof IllegalArgumentException || error instanceof ValidationException) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
}