package dev.tsvinc.r2p.api.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record InitiateR2pResponse(
        @NotBlank String responseMessageId,
        @NotEmpty List<PaymentRequestMinResponse> paymentRequests,
        @NotBlank String requestMessageId,
        @NotBlank String creationDateTime
) {
}
