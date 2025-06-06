package dev.tsvinc.r2p.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record PaymentRequest(
        @DecimalMin(value = "0.01") BigDecimal requestedAmount
) {}
