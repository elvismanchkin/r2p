package dev.tsvinc.r2p.client.dto.response;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// Multiple Retrieve Response
public record MultipleRetrieveR2pResponse(
        @NotEmpty List<RetrieveR2pResponse> paymentRequestDetails
) {
}
