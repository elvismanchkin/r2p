package dev.tsvinc.r2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RefundR2pRequest(
        @NotEmpty @Size(max = 1) @Valid List<RefundPaymentRequest> paymentRequests,
        @NotBlank @Size(max = 35) String requestMessageId,
        @NotEmpty @Valid List<SettlementOption> settlementOptions,
        @NotNull @Valid Creditor creditor,
        @NotBlank String creationDateTime
) {
}
