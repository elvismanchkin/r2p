package dev.tsvinc.r2p;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record AmendR2pResponse(
        @NotBlank String responseMessageId,
        @NotBlank String paymentRequestId,
        @NotBlank String requestMessageId,
        @NotNull TransactionStatus transactionStatus,
        @NotBlank String creationDateTime
) {
    public static AmendR2pResponse create(String paymentRequestId, AmendR2pRequest request, TransactionStatus status) {
        return new AmendR2pResponse(
                UUID.randomUUID().toString(),
                paymentRequestId,
                request.requestMessageId(),
                status,
                Instant.now().toString()
        );
    }
}
