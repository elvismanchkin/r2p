package dev.tsvinc.r2p.api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AmendR2pRequest(
        @NotBlank @Size(max = 35) String requestMessageId,
        @Size(max = 10) @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String dueDate,
        @Valid RequestReason requestReason,
        @Valid PaymentRequest paymentRequest,
        @Valid List<SettlementOption> settlementOptions,
        @Valid RequestOptions requestOptions,
        @NotBlank String creationDateTime
) {
}
