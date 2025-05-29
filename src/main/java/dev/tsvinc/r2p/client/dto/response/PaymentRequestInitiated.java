package dev.tsvinc.r2p.client.dto.response;

import jakarta.validation.constraints.NotBlank;

// Payment Request Initiated (for retrieve response)
public record PaymentRequestInitiated(
        @NotBlank String paymentRequestId,
        @NotBlank String endToEndId,
        @NotBlank String debtorAgentId,
        @NotBlank String debtorCountry,
        @NotBlank String debtorAgentCountry,
        @NotBlank String debtorFirstName,
        @NotBlank String debtorLastName,
        @NotBlank Double requestedAmount,
        @NotBlank String requestedAmountCurrency,
        String debtorAlias,
        String debtorAliasType,
        Double acceptedAmount,
        String acceptedAmountCurrency,
        String debtorId,
        String debtorIdType,
        String debtorBusinessName,
        String paymentRequestType,
        String originalPaymentRequestId) {}
