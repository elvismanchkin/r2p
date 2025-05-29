package dev.tsvinc.r2p.client.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

// Retrieve by Payment Request IDs
public record RetrieveR2pByPaymentRequestIdsRequest(
        @NotBlank @Size(max = 35) String agentId,
        @NotEmpty @Size(max = 10) List<@Size(max = 35) String> paymentRequestIds
) {
}

