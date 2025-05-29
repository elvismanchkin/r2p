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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

    @Operation(summary = "Initiate R2P", description = "Create a new Request to Pay transaction")
    @ApiResponse(responseCode = "201", description = "R2P created successfully")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<InitiateR2pResponse>> initiateR2P(
            @RequestHeader("Content-Type") String contentType,
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
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .headers(headers -> {
                            headers.add("x-request-affinity", affinity);
                            headers.add("x-correlation-id", corrId);
                            headers.add("x-processing-time",
                                    String.valueOf(System.currentTimeMillis() - startTime.toEpochMilli()));
                            headers.add("Location", "/rtx/api/v1/requestToPay/" +
                                    response.paymentRequests().get(0).paymentRequestId());
                        })
                        .body(response))
                .doOnSuccess(response -> log.info("R2P initiated: {} (correlation: {})",
                        response.getBody().responseMessageId(), corrId));
    }

    @Operation(summary = "Retrieve R2P", description = "Retrieve a single Request to Pay by payment request ID")
    @ApiResponse(responseCode = "200", description = "R2P retrieved successfully")
    @GetMapping(value = "/{paymentRequestId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<RetrieveR2pResponse>> retrieveR2P(
            @Parameter(description = "Payment request ID", required = true)
            @PathVariable String paymentRequestId,
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("keyID") String keyId,
            @RequestHeader(value = "x-request-affinity", required = false) String requestAffinity) {

        return webClientService.retrieveR2P(paymentRequestId, keyId, requestAffinity)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Retrieved R2P: {}", paymentRequestId));
    }

    /*TODO: implement enhanced headers for other methods*/
    @Operation(summary = "Retrieve Multiple R2P", description = "Retrieve multiple Request to Pay transactions with pagination")
    @PostMapping(value = "/retrieve",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<R2PApiResponse<MultipleRetrieveR2pResponse>>> retrieveMultipleR2P(
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("keyID") String keyId,
            @RequestHeader(value = "x-request-affinity", required = false) String requestAffinity,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @Valid @RequestBody Object request) {

        String corrId = correlationId != null ? correlationId : UUID.randomUUID().toString();
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
                            .processingTime(String.valueOf(System.currentTimeMillis() - startTime.toEpochMilli()) + "ms")
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
                                headers.add("x-correlation-id", corrId);
                                headers.add("X-Total-Count", String.valueOf(totalElements));
                                headers.add("X-Page-Count", String.valueOf(totalPages));
                                headers.add("Link", buildLinkHeader(page, size, totalPages));
                            })
                            .body(apiResponse);
                });
    }


    @Operation(summary = "Confirm R2P", description = "Confirm a Request to Pay transaction")
    @ApiResponse(responseCode = "200", description = "R2P confirmed successfully")
    @PatchMapping(value = "/{paymentRequestId}/confirm",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ConfirmR2pResponse>> confirmR2P(
            @Parameter(description = "Payment request ID", required = true)
            @PathVariable String paymentRequestId,
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("keyID") String keyId,
            @RequestHeader("x-request-affinity") String requestAffinity,
            @Valid @RequestBody ConfirmR2pRequest request) {

        return webClientService.confirmR2P(paymentRequestId, keyId, requestAffinity, request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("R2P confirmed: {} with status {}",
                        paymentRequestId, request.transactionStatus()));
    }

    @Operation(summary = "Cancel R2P", description = "Cancel a Request to Pay transaction")
    @ApiResponse(responseCode = "200", description = "R2P cancelled successfully")
    @PatchMapping(value = "/{paymentRequestId}/cancel",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<CancelR2pResponse>> cancelR2P(
            @Parameter(description = "Payment request ID", required = true)
            @PathVariable String paymentRequestId,
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("keyID") String keyId,
            @RequestHeader("x-request-affinity") String requestAffinity,
            @Valid @RequestBody CancelR2pRequest request) {

        return webClientService.cancelR2P(paymentRequestId, keyId, requestAffinity, request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("R2P cancelled: {} with reason {}",
                        paymentRequestId, request.cancellationReason()));
    }

    @Operation(summary = "Amend R2P", description = "Amend a Request to Pay transaction")
    @ApiResponse(responseCode = "200", description = "R2P amended successfully")
    @PatchMapping(value = "/{paymentRequestId}/amend",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<AmendR2pResponse>> amendR2P(
            @Parameter(description = "Payment request ID", required = true)
            @PathVariable String paymentRequestId,
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("keyID") String keyId,
            @RequestHeader("x-request-affinity") String requestAffinity,
            @Valid @RequestBody AmendR2pRequest request) {

        return webClientService.amendR2P(paymentRequestId, keyId, requestAffinity, request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("R2P amended: {}", paymentRequestId));
    }

    @Operation(summary = "Refund R2P", description = "Create a refund Request to Pay")
    @ApiResponse(responseCode = "201", description = "Refund R2P created successfully")
    @PostMapping(value = "/{originalPaymentRequestId}/refund",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<RefundR2pResponse>> refundR2P(
            @Parameter(description = "Original payment request ID", required = true)
            @PathVariable String originalPaymentRequestId,
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("keyID") String keyId,
            @RequestHeader(value = "x-request-affinity", required = false) String requestAffinity,
            @Valid @RequestBody RefundR2pRequest request) {

        return webClientService.refundR2P(originalPaymentRequestId, keyId, requestAffinity, request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .doOnSuccess(response -> log.info("Refund R2P created for: {}", originalPaymentRequestId));
    }

    @Operation(summary = "Transaction Tagging", description = "Tag a transaction with acknowledgment information")
    @ApiResponse(responseCode = "200", description = "Transaction tagged successfully")
    @PostMapping(value = "/transaction/tag", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Void>> transactionTagging(
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("keyID") String keyId,
            @RequestHeader(value = "x-request-affinity", required = false) String requestAffinity,
            @Valid @RequestBody TransactionTaggingRequest request) {

        return webClientService.submitTransactionTagging(keyId, requestAffinity, request)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .doOnSuccess(v -> log.info("Transaction tagged: {}",
                        request.taggedTransaction().transactionId()));
    }

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
}