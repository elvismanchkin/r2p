package dev.tsvinc.r2p.api.controller;

import dev.tsvinc.r2p.api.dto.request.AmendR2pRequest;
import dev.tsvinc.r2p.api.dto.request.CancelR2pRequest;
import dev.tsvinc.r2p.api.dto.request.ConfirmR2pRequest;
import dev.tsvinc.r2p.api.dto.request.InitiateR2pRequest;
import dev.tsvinc.r2p.api.dto.request.RefundR2pRequest;
import dev.tsvinc.r2p.api.dto.request.TransactionTaggingRequest;
import dev.tsvinc.r2p.api.dto.response.AmendR2pResponse;
import dev.tsvinc.r2p.api.dto.response.CancelR2pResponse;
import dev.tsvinc.r2p.api.dto.response.ConfirmR2pResponse;
import dev.tsvinc.r2p.api.dto.response.InitiateR2pResponse;
import dev.tsvinc.r2p.api.dto.response.R2PApiResponse;
import dev.tsvinc.r2p.api.dto.response.RefundR2pResponse;
import dev.tsvinc.r2p.client.dto.request.RetrieveR2pByEndToEndIdsRequest;
import dev.tsvinc.r2p.client.dto.request.RetrieveR2pByPaymentRequestIdsRequest;
import dev.tsvinc.r2p.client.dto.response.MultipleRetrieveR2pResponse;
import dev.tsvinc.r2p.client.dto.response.RetrieveR2pResponse;
import dev.tsvinc.r2p.client.service.R2PWebClientService;
import dev.tsvinc.r2p.service.validation.R2PBusinessValidationService;
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
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/rtx/api/v1/requestToPay")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Request to Pay", description = "Complete R2P API with enhanced headers and metadata")
public class RequestToPayInboundController {

    private final R2PWebClientService webClientService;
    private final R2PBusinessValidationService validationService;

    private void addSecurityHeaders(ResponseEntity.HeadersBuilder<?> headers) {
        headers
            .header("X-Content-Type-Options", "nosniff")
            .header("X-Frame-Options", "DENY")
            .header("X-XSS-Protection", "1; mode=block")
            .header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
            .header("Content-Security-Policy", "default-src 'self'")
            .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    }

    private void addDistributedTracingHeaders(ResponseEntity.HeadersBuilder<?> headers, String correlationId) {
        headers
            .header("X-Correlation-ID", correlationId)
            .header("X-Request-ID", UUID.randomUUID().toString())
            .header("X-Instance-ID", System.getProperty("instance.id", "unknown"));
    }

    @Operation(summary = "Initiate R2P", description = "Create a new Request to Pay transaction")
    @ApiResponse(responseCode = "201", description = "R2P created successfully")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<InitiateR2pResponse>> initiateR2P(
            @RequestHeader("keyID") String keyId,
            @RequestHeader(value = "x-request-affinity", required = false) String requestAffinity,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @Valid @RequestBody InitiateR2pRequest request) {

        Instant startTime = Instant.now();
        String affinity = requestAffinity != null ? requestAffinity : UUID.randomUUID().toString();
        String corrId = correlationId != null ? correlationId : UUID.randomUUID().toString();

        // Business validation
        validationService.validateInitiateRequest(request);

        return webClientService.initiateR2P(keyId, affinity, request)
                .map(response -> {
                    ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(HttpStatus.CREATED);
                    addSecurityHeaders(responseBuilder);
                    addDistributedTracingHeaders(responseBuilder, corrId);
                    
                    return responseBuilder
                            .headers(headers -> {
                                headers.add("x-request-affinity", affinity);
                                headers.add("x-processing-time",
                                        String.valueOf(System.currentTimeMillis() - startTime.toEpochMilli()));
                                headers.add("Location", "/rtx/api/v1/requestToPay/" +
                                        response.paymentRequests().getFirst().paymentRequestId());
                            })
                            .body(response);
                })
                .doOnError(error -> log.error("Error initiating R2P: {} (correlation: {})", error.getMessage(), corrId))
                .doOnSuccess(response -> {
                    if (response != null && response.getBody() != null) {
                        log.info("R2P initiated: {} (correlation: {})",
                                response.getBody().responseMessageId(), corrId);
                    } else {
                        log.warn("R2P initiated but response or response body was null (correlation: {})", corrId);
                    }
                });
    }

    @Operation(summary = "Retrieve R2P", description = "Retrieve a single Request to Pay by payment request ID")
    @ApiResponse(responseCode = "200", description = "R2P retrieved successfully")
    @GetMapping(value = "/{paymentRequestId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<RetrieveR2pResponse>> retrieveR2P(
            @Parameter(description = "Payment request ID", required = true)
            @PathVariable String paymentRequestId,
            @RequestHeader("keyID") String keyId,
            @RequestHeader(value = "x-request-affinity", required = false) String requestAffinity,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId) {

        Instant startTime = Instant.now();
        String affinity = requestAffinity != null ? requestAffinity : UUID.randomUUID().toString();
        String corrId = correlationId != null ? correlationId : UUID.randomUUID().toString();

        return webClientService.retrieveR2P(paymentRequestId, keyId, requestAffinity)
                .map(response -> {
                    ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
                    addSecurityHeaders(responseBuilder);
                    addDistributedTracingHeaders(responseBuilder, corrId);
                    
                    return responseBuilder
                            .headers(headers -> {
                                headers.add("x-request-affinity", affinity);
                                headers.add("x-processing-time",
                                        String.valueOf(System.currentTimeMillis() - startTime.toEpochMilli()));
                            })
                            .body(response);
                })
                .doOnError(error -> log.error("Error retrieving R2P: {} (correlation: {})", error.getMessage(), corrId))
                .doOnSuccess(response -> {
                    if (response != null && response.getBody() != null) {
                        log.info("R2P retrieved: {} (correlation: {})", paymentRequestId, corrId);
                    } else {
                        log.warn("R2P retrieved but response or response body was null (correlation: {})", corrId);
                    }
                });
    }

    /*TODO: implement enhanced headers for other methods*/
    @Operation(summary = "Retrieve Multiple R2P", description = "Retrieve multiple Request to Pay transactions with pagination")
    @PostMapping(value = "/retrieve",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<R2PApiResponse<MultipleRetrieveR2pResponse>>> retrieveMultipleR2P(
            @RequestHeader("keyID") String keyId,
            @RequestHeader(value = "x-request-affinity", required = false) String requestAffinity,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @Valid @RequestBody Object request) {

        String corrId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        String affinity = requestAffinity != null ? requestAffinity : UUID.randomUUID().toString();
        Instant startTime = Instant.now();

        Mono<MultipleRetrieveR2pResponse> responseMono;
        if (request instanceof RetrieveR2pByPaymentRequestIdsRequest paymentIdsRequest) {
            responseMono = webClientService.retrieveMultipleR2PByPaymentRequestIds(keyId, requestAffinity, paymentIdsRequest);
        } else if (request instanceof RetrieveR2pByEndToEndIdsRequest endToEndIdsRequest) {
            responseMono = webClientService.retrieveMultipleR2PByEndToEndIds(keyId, requestAffinity, endToEndIdsRequest);
        } else {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return responseMono
                .map(response -> {
                    // Calculate pagination (simplified)
                    int totalElements = response.paymentRequestDetails().size();
                    int totalPages = (int) Math.ceil((double) totalElements / size);

                    R2PApiResponse.PaginationInfo pagination = R2PApiResponse.PaginationInfo.builder()
                            .page(page)
                            .size(size)
                            .totalElements(totalElements)
                            .totalPages(totalPages)
                            .hasNext(page < totalPages - 1)
                            .hasPrevious(page > 0)
                            .build();

                    R2PApiResponse.ResponseMetadata metadata = R2PApiResponse.ResponseMetadata.builder()
                            .requestId(UUID.randomUUID().toString())
                            .correlationId(corrId)
                            .timestamp(Instant.now().toString())
                            .apiVersion("v1")
                            .processingTime(System.currentTimeMillis() - startTime.toEpochMilli() + "ms")
                            .headers(Map.of(
                                    "X-Total-Count", String.valueOf(totalElements),
                                    "X-Page-Count", String.valueOf(totalPages)
                            ))
                            .build();

                    R2PApiResponse<MultipleRetrieveR2pResponse> apiResponse = R2PApiResponse.<MultipleRetrieveR2pResponse>builder()
                            .data(response)
                            .metadata(metadata)
                            .pagination(pagination)
                            .build();

                    return ResponseEntity.ok()
                            .headers(headers -> {
                                headers.add("x-request-affinity", affinity);
                                headers.add("x-correlation-id", corrId);
                                headers.add("x-processing-time",
                                        String.valueOf(System.currentTimeMillis() - startTime.toEpochMilli()));
                                headers.add("X-Total-Count", String.valueOf(totalElements));
                                headers.add("X-Page-Count", String.valueOf(totalPages));
                                headers.add("Link", buildLinkHeader(page, size, totalPages));
                            })
                            .body(apiResponse);
                })
                .doOnError(error -> log.error("Error retrieving multiple R2P: {}", error.getMessage()))
                .doOnSuccess(response -> {
                    if (response != null && response.getBody() != null) {
                        log.info("Multiple R2P retrieved successfully (correlation: {})", corrId);
                    } else {
                        log.warn("Multiple R2P retrieved but response or response body was null (correlation: {})", corrId);
                    }
                });
    }

    /**
     * Builds an HTTP Link header string for pagination following RFC 5988 standard.
     * The header includes links to navigate between pages (prev, next, first, last).
     *
     * @param page Current page number (0-based)
     * @param size Number of items per page
     * @param totalPages Total number of pages available
     * @return A formatted string containing pagination links in the format:
     *         <url>; rel="type", where type can be "prev", "next", "first", or "last"
     */
    private String buildLinkHeader(int page, int size, int totalPages) {
        StringBuilder links = new StringBuilder();
        String baseUrl = "/rtx/api/v1/requestToPay/retrieve";

        if (page > 0) {
            links.append(String.format("<%s?page=%d&size=%d>; rel=\"prev\", ", baseUrl, page - 1, size));
        }
        if (page < totalPages - 1) {
            links.append(String.format("<%s?page=%d&size=%d>; rel=\"next\", ", baseUrl, page + 1, size));
        }
        links.append(String.format("<%s?page=0&size=%d>; rel=\"first\", ", baseUrl, size));
        links.append(String.format("<%s?page=%d&size=%d>; rel=\"last\"", baseUrl, totalPages - 1, size));

        return links.toString();
    }

    @Operation(summary = "Confirm R2P", description = "Confirm a Request to Pay transaction")
    @ApiResponse(responseCode = "200", description = "R2P confirmed successfully")
    @PatchMapping(value = "/{paymentRequestId}/confirm",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ConfirmR2pResponse>> confirmR2P(
            @Parameter(description = "Payment request ID", required = true)
            @PathVariable String paymentRequestId,
            @RequestHeader("keyID") String keyId,
            @RequestHeader("x-request-affinity") String requestAffinity,
            @Valid @RequestBody ConfirmR2pRequest request) {

        // Add validation
        validationService.validateConfirmRequest(request);

        return webClientService.confirmR2P(paymentRequestId, keyId, requestAffinity, request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> {
                    if (response != null && response.getBody() != null) {
                        log.info("R2P confirmed: {} with status {}",
                                paymentRequestId, request.transactionStatus());
                    } else {
                        log.warn("R2P confirmed but response or response body was null for paymentRequestId: {}", paymentRequestId);
                    }
                });
    }

    @Operation(summary = "Cancel R2P", description = "Cancel a Request to Pay transaction")
    @ApiResponse(responseCode = "200", description = "R2P cancelled successfully")
    @PatchMapping(value = "/{paymentRequestId}/cancel",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<CancelR2pResponse>> cancelR2P(
            @Parameter(description = "Payment request ID", required = true)
            @PathVariable String paymentRequestId,
            @RequestHeader("keyID") String keyId,
            @RequestHeader("x-request-affinity") String requestAffinity,
            @Valid @RequestBody CancelR2pRequest request) {

        // Add validation
        validationService.validateCancelRequest(request);

        return webClientService.cancelR2P(paymentRequestId, keyId, requestAffinity, request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> {
                    if (response != null && response.getBody() != null) {
                        log.info("R2P cancelled: {} with reason {}",
                                paymentRequestId, request.cancellationReason());
                    } else {
                        log.warn("R2P cancelled but response or response body was null for paymentRequestId: {}", paymentRequestId);
                    }
                });
    }

    @Operation(summary = "Amend R2P", description = "Amend a Request to Pay transaction")
    @ApiResponse(responseCode = "200", description = "R2P amended successfully")
    @PatchMapping(value = "/{paymentRequestId}/amend",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<AmendR2pResponse>> amendR2P(
            @Parameter(description = "Payment request ID", required = true)
            @PathVariable String paymentRequestId,
            @RequestHeader("keyID") String keyId,
            @RequestHeader("x-request-affinity") String requestAffinity,
            @Valid @RequestBody AmendR2pRequest request) {

        // Add validation
        validationService.validateAmendRequest(request);

        return webClientService.amendR2P(paymentRequestId, keyId, requestAffinity, request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> {
                    if (response != null && response.getBody() != null) {
                        log.info("R2P amended: {}", paymentRequestId);
                    } else {
                        log.warn("R2P amended but response or response body was null for paymentRequestId: {}", paymentRequestId);
                    }
                });
    }

    @Operation(summary = "Refund R2P", description = "Create a refund Request to Pay")
    @ApiResponse(responseCode = "201", description = "Refund R2P created successfully")
    @PostMapping(value = "/{originalPaymentRequestId}/refund",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<RefundR2pResponse>> refundR2P(
            @Parameter(description = "Original payment request ID", required = true)
            @PathVariable String originalPaymentRequestId,
            @RequestHeader("keyID") String keyId,
            @RequestHeader(value = "x-request-affinity", required = false) String requestAffinity,
            @Valid @RequestBody RefundR2pRequest request) {

        // Add validation
        validationService.validateRefundRequest(request);

        return webClientService.refundR2P(originalPaymentRequestId, keyId, requestAffinity, request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .doOnSuccess(response -> {
                    if (response != null && response.getBody() != null) {
                        log.info("Refund R2P created for: {}", originalPaymentRequestId);
                    } else {
                        log.warn("Refund R2P created but response or response body was null for originalPaymentRequestId: {}", originalPaymentRequestId);
                    }
                });
    }

    @Operation(summary = "Transaction Tagging", description = "Tag a transaction with acknowledgment information")
    @ApiResponse(responseCode = "200", description = "Transaction tagged successfully")
    @PostMapping(value = "/transaction/tag", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Void>> transactionTagging(
            @RequestHeader("keyID") String keyId,
            @RequestHeader(value = "x-request-affinity", required = false) String requestAffinity,
            @Valid @RequestBody TransactionTaggingRequest request) {

        return webClientService.submitTransactionTagging(keyId, requestAffinity, request)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .doOnSuccess(v -> {
                    if (request != null && request.taggedTransaction() != null) {
                        log.info("Transaction tagged: {}", request.taggedTransaction().transactionId());
                    } else {
                        log.warn("Transaction tagged but request or taggedTransaction was null");
                    }
                });
    }
}