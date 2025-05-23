package dev.tsvinc.r2p;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentRequestMinResponse(
        @NotBlank String paymentRequestId,
        @NotBlank String endToEndId,
        @NotNull TransactionStatus transactionStatus,
        String debtorId,
        String debtorIdType) {}
