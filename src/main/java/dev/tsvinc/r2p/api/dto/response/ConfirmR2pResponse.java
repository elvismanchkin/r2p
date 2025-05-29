package dev.tsvinc.r2p.api.dto.response;

import dev.tsvinc.r2p.api.dto.request.ConfirmR2pRequest;
import dev.tsvinc.r2p.domain.enums.TransactionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record ConfirmR2pResponse(
        @NotBlank String responseMessageId,
        @NotBlank String paymentRequestId,
        @NotBlank String endToEndId,
        @NotBlank String requestMessageId,
        @NotNull TransactionStatus transactionStatus,
        @NotBlank String creationDateTime
) {
    public static ConfirmR2pResponse create(ConfirmR2pRequest request) {
        return new ConfirmR2pResponse(
                UUID.randomUUID().toString(),
                request.paymentRequestId(),
                request.endToEndId(),
                request.requestMessageId(),
                request.transactionStatus(),
                Instant.now().toString()
        );
    }
}