package dev.tsvinc.r2p;

import jakarta.validation.constraints.DecimalMin;

public record PaymentRequest(@DecimalMin(value = "0.01") Double requestedAmount) {}
