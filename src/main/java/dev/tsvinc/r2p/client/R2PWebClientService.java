package dev.tsvinc.r2p.client;

import dev.tsvinc.r2p.AmendR2pRequest;
import dev.tsvinc.r2p.AmendR2pResponse;
import dev.tsvinc.r2p.CancelR2pRequest;
import dev.tsvinc.r2p.CancelR2pResponse;
import dev.tsvinc.r2p.ConfirmR2pRequest;
import dev.tsvinc.r2p.ConfirmR2pResponse;
import dev.tsvinc.r2p.InitiateR2pRequest;
import dev.tsvinc.r2p.InitiateR2pResponse;
import dev.tsvinc.r2p.RefundR2pRequest;
import dev.tsvinc.r2p.RefundR2pResponse;
import dev.tsvinc.r2p.TransactionTaggingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
public class R2PWebClientService {

    @Qualifier("r2pWebClient")
    private final WebClient webClient;

    public R2PWebClientService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Initiate a new Request to Pay
     */
    public Mono<InitiateR2pResponse> initiateR2P(String keyId, String requestAffinity, InitiateR2pRequest request) {
        return webClient.post()
                .uri("/rtx/api/v1/requestToPay")
                .header("keyID", keyId)
                .header("x-request-affinity", requestAffinity)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(InitiateR2pResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .doOnSuccess(response -> log.info("R2P initiated successfully: {}", response.responseMessageId()))
                .doOnError(error -> log.error("Failed to initiate R2P: {}", error.getMessage()));
    }

    /**
     * Retrieve a single Request to Pay by payment request ID
     */
    public Mono<RetrieveR2pResponse> retrieveR2P(String paymentRequestId, String keyId, String requestAffinity) {
        return webClient.get()
                .uri("/rtx/api/v1/requestToPay/{paymentRequestId}", paymentRequestId)
                .header("keyID", keyId)
                .header("x-request-affinity", requestAffinity)
                .retrieve()
                .bodyToMono(RetrieveR2pResponse.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .doOnSuccess(response -> log.debug("Retrieved R2P: {}", paymentRequestId))
                .doOnError(error -> log.error("Failed to retrieve R2P {}: {}", paymentRequestId, error.getMessage()));
    }

    /**
     * Retrieve multiple Request to Pay transactions by payment request IDs
     */
    public Mono<MultipleRetrieveR2pResponse> retrieveMultipleR2PByPaymentRequestIds(
            String keyId, String requestAffinity, RetrieveR2pByPaymentRequestIdsRequest request) {
        return webClient.post()
                .uri("/rtx/api/v1/requestToPay/retrieve")
                .header("keyID", keyId)
                .header("x-request-affinity", requestAffinity)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MultipleRetrieveR2pResponse.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .doOnSuccess(response -> log.debug("Retrieved {} R2P transactions",
                        response.paymentRequestDetails().size()))
                .doOnError(error -> log.error("Failed to retrieve multiple R2P: {}", error.getMessage()));
    }

    /**
     * Retrieve multiple Request to Pay transactions by end-to-end IDs
     */
    public Mono<MultipleRetrieveR2pResponse> retrieveMultipleR2PByEndToEndIds(
            String keyId, String requestAffinity, RetrieveR2pByEndToEndIdsRequest request) {
        return webClient.post()
                .uri("/rtx/api/v1/requestToPay/retrieve")
                .header("keyID", keyId)
                .header("x-request-affinity", requestAffinity)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MultipleRetrieveR2pResponse.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .doOnSuccess(response -> log.debug("Retrieved {} R2P transactions by endToEndIds",
                        response.paymentRequestDetails().size()))
                .doOnError(error -> log.error("Failed to retrieve multiple R2P by endToEndIds: {}", error.getMessage()));
    }

    /**
     * Confirm a Request to Pay
     */
    public Mono<ConfirmR2pResponse> confirmR2P(String paymentRequestId, String keyId,
                                               String requestAffinity, ConfirmR2pRequest request) {
        return webClient.patch()
                .uri("/rtx/api/v1/requestToPay/{paymentRequestId}/confirm", paymentRequestId)
                .header("keyID", keyId)
                .header("x-request-affinity", requestAffinity)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ConfirmR2pResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .doOnSuccess(response -> log.info("R2P confirmed: {} with status {}",
                        paymentRequestId, request.transactionStatus()))
                .doOnError(error -> log.error("Failed to confirm R2P {}: {}", paymentRequestId, error.getMessage()));
    }

    /**
     * Cancel a Request to Pay
     */
    public Mono<CancelR2pResponse> cancelR2P(String paymentRequestId, String keyId,
                                             String requestAffinity, CancelR2pRequest request) {
        return webClient.patch()
                .uri("/rtx/api/v1/requestToPay/{paymentRequestId}/cancel", paymentRequestId)
                .header("keyID", keyId)
                .header("x-request-affinity", requestAffinity)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CancelR2pResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .doOnSuccess(response -> log.info("R2P cancelled: {} with reason {}",
                        paymentRequestId, request.cancellationReason()))
                .doOnError(error -> log.error("Failed to cancel R2P {}: {}", paymentRequestId, error.getMessage()));
    }

    /**
     * Amend a Request to Pay
     */
    public Mono<AmendR2pResponse> amendR2P(String paymentRequestId, String keyId,
                                           String requestAffinity, AmendR2pRequest request) {
        return webClient.patch()
                .uri("/rtx/api/v1/requestToPay/{paymentRequestId}/amend", paymentRequestId)
                .header("keyID", keyId)
                .header("x-request-affinity", requestAffinity)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AmendR2pResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .doOnSuccess(response -> log.info("R2P amended: {}", paymentRequestId))
                .doOnError(error -> log.error("Failed to amend R2P {}: {}", paymentRequestId, error.getMessage()));
    }

    /**
     * Create a refund Request to Pay
     */
    public Mono<RefundR2pResponse> refundR2P(String originalPaymentRequestId, String keyId,
                                             String requestAffinity, RefundR2pRequest request) {
        return webClient.post()
                .uri("/rtx/api/v1/requestToPay/{originalPaymentRequestId}/refund", originalPaymentRequestId)
                .header("keyID", keyId)
                .header("x-request-affinity", requestAffinity)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RefundR2pResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .doOnSuccess(response -> log.info("R2P refund created for: {}", originalPaymentRequestId))
                .doOnError(error -> log.error("Failed to create R2P refund for {}: {}",
                        originalPaymentRequestId, error.getMessage()));
    }

    /**
     * Submit transaction tagging
     */
    public Mono<Void> submitTransactionTagging(String keyId, String requestAffinity,
                                               TransactionTaggingRequest request) {
        return webClient.post()
                .uri("/rtx/api/v1/requestToPay/transaction/tag")
                .header("keyID", keyId)
                .header("x-request-affinity", requestAffinity)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .then()
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .doOnSuccess(v -> log.info("Transaction tagging submitted for: {}",
                        request.taggedTransaction().transactionId()))
                .doOnError(error -> log.error("Failed to submit transaction tagging: {}", error.getMessage()));
    }

    /**
     * Get reference data (available participants)
     */
    public Mono<ReferenceDataResponse> getReferenceData(String keyId, ReferenceDataRequest request) {
        return webClient.post()
                .uri("/rtx/api/v1/requestToPay/referenceData")
                .header("keyID", keyId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ReferenceDataResponse.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .doOnSuccess(response -> log.debug("Retrieved reference data with {} participants",
                        response.availableParticipants().size()))
                .doOnError(error -> log.error("Failed to get reference data: {}", error.getMessage()));
    }

    /**
     * View active blocks for a debtor
     */
    public Mono<ViewBlockResponse> viewBlocks(String keyId, ViewBlockRequest request) {
        return webClient.post()
                .uri("/rtx/api/v1/requestControl/view")
                .header("keyID", keyId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ViewBlockResponse.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .doOnSuccess(response -> log.debug("Retrieved {} active blocks for debtor: {}",
                        response.blockedPayees().size(), request.debtorId()))
                .doOnError(error -> log.error("Failed to view blocks: {}", error.getMessage()));
    }

    /**
     * Remove a block
     */
    public Mono<Void> removeBlock(String blockReferenceId, String keyId) {
        return webClient.patch()
                .uri("/rtx/api/v1/requestControl/{blockReferenceId}/remove", blockReferenceId)
                .header("keyID", keyId)
                .retrieve()
                .toBodilessEntity()
                .then()
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .doOnSuccess(v -> log.info("Block removed: {}", blockReferenceId))
                .doOnError(error -> log.error("Failed to remove block {}: {}", blockReferenceId, error.getMessage()));
    }

    /**
     * Helper method to generate request message ID
     */
    public String generateRequestMessageId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 35);
    }
}