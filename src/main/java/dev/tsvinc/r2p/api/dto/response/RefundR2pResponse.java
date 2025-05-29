package dev.tsvinc.r2p.api.dto.response;

import dev.tsvinc.r2p.api.dto.request.RefundR2pRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RefundR2pResponse(
        @NotBlank String responseMessageId,
        @NotEmpty List<PaymentRequestMinResponse> paymentRequests,
        @NotBlank String requestMessageId,
        @NotBlank String creationDateTime
) {
    public static RefundR2pResponse create(RefundR2pRequest request, List<PaymentRequestMinResponse> paymentRequests) {
        return new RefundR2pResponse(
                UUID.randomUUID().toString(),
                paymentRequests,
                request.requestMessageId(),
                Instant.now().toString()
        );
    }
}
