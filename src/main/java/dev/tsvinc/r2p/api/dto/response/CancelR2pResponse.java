package dev.tsvinc.r2p.api.dto.response;

import dev.tsvinc.r2p.api.dto.request.CancelR2pRequest;
import dev.tsvinc.r2p.domain.enums.TransactionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CancelR2pResponse(
        @NotBlank String responseMessageId,
        @NotBlank String requestMessageId,
        @NotNull TransactionStatus transactionStatus,
        @NotBlank String creationDateTime
) {
    public static CancelR2pResponse create(CancelR2pRequest request) {
        return new CancelR2pResponse(
                UUID.randomUUID().toString(),
                request.requestMessageId(),
                TransactionStatus.CNCL,
                Instant.now().toString()
        );
    }
}
