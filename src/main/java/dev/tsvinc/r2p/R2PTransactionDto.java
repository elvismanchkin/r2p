package dev.tsvinc.r2p;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for R2P Transaction data.
 */
public record R2PTransactionDto(
        String paymentRequestId,
        String endToEndId,
        TransactionStatus transactionStatus,
        UseCase useCase,
        BigDecimal requestedAmount,
        String requestedAmountCurrency,
        BigDecimal acceptedAmount,
        String acceptedAmountCurrency,
        String creditorAgentId,
        String debtorAgentId,
        LocalDateTime dueDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
