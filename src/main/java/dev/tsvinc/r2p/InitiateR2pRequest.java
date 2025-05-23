package dev.tsvinc.r2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

// InitiateR2pRequest and InitiateR2pResponse
@JsonIgnoreProperties(ignoreUnknown = true)
public record InitiateR2pRequest(
        @NotNull Product product,
        @NotNull UseCase useCase,
        @Valid RequestReason requestReason,
        @NotEmpty @Size(max = 1) @Valid List<PaymentRequestDetail> paymentRequests,
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String dueDate,
        @NotBlank @Size(max = 35) String requestMessageId,
        @NotEmpty @Valid List<SettlementOption> settlementOptions,
        @NotNull @Valid Creditor creditor,
        @Valid RequestOptions requestOptions,
        @NotBlank String creationDateTime
) {
}
