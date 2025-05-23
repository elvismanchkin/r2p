package dev.tsvinc.r2p;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PaymentRequestDetail(
        @NotBlank @Size(max = 35) String endToEndId,
        @NotBlank @Size(max = 35) String debtorAlias,
        @NotNull AliasType debtorAliasType,
        @NotBlank @Size(max = 35) String debtorAgentId,
        @NotBlank @Size(min = 2, max = 2) String debtorCountry,
        @NotBlank @Size(min = 2, max = 2) String debtorAgentCountry,
        @NotBlank @Size(max = 140) String debtorFirstName,
        @NotBlank @Size(max = 140) @Pattern(regexp = "^[A-Z]\\.?$") String debtorLastName,
        @NotNull @DecimalMin(value = "0.01") Double requestedAmount,
        @NotBlank @Size(min = 3, max = 3) String requestedAmountCurrency,
        @NotBlank @Size(max = 35) String paymentRequestId) {}
