package dev.tsvinc.r2p.api.dto.request;

import dev.tsvinc.r2p.domain.enums.AliasType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;


public record PaymentRequestDetail(
        @NotBlank @Size(max = 35) String endToEndId,
        @NotBlank @Size(max = 35) String debtorAlias,
        @NotNull AliasType debtorAliasType,
        @NotBlank @Size(max = 35) String debtorAgentId,
        @NotBlank @Size(min = 2, max = 2) String debtorCountry,
        @NotBlank @Size(min = 2, max = 2) String debtorAgentCountry,
        @NotBlank @Size(max = 140) String debtorFirstName,
        @NotBlank @Size(max = 140) @Pattern(regexp = "^[A-Z]\\.?$") String debtorLastName,
        @NotNull @DecimalMin(value = "0.01") BigDecimal requestedAmount,
        @NotBlank @Size(min = 3, max = 3) String requestedAmountCurrency,
        // Optional fields from Swagger
        @Size(max = 35) String debtorId,
        @Pattern(regexp = "AGENT|AGENTPR") String debtorIdType,
        @Size(max = 140) String debtorBusinessName // for B2C refunds
) {
}
