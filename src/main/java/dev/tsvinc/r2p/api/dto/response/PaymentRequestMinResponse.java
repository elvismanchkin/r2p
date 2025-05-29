package dev.tsvinc.r2p.api.dto.response;

import dev.tsvinc.r2p.domain.enums.TransactionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentRequestMinResponse(
        @NotBlank String paymentRequestId,
        @NotBlank String endToEndId,
        @NotNull TransactionStatus transactionStatus,
        String debtorId,
        String debtorIdType) {}
