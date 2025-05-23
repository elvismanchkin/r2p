package dev.tsvinc.r2p;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RefundPaymentRequest(
        @NotBlank @Size(max = 35) String endToEndId,
        @NotNull @DecimalMin(value = "0.01") Double requestedAmount
) {
}
