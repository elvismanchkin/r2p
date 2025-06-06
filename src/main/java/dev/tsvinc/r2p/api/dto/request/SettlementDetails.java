package dev.tsvinc.r2p.api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.tsvinc.r2p.domain.enums.SettlementSystem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SettlementDetails(
        @NotNull SettlementSystem settlementSystem,
        @NotBlank @Size(max = 36) String settlementSystemReferenceId,
        @Pattern(regexp = "RRN|TXNID|YPPID") String settlementSystemReferenceType,
        @NotNull @DecimalMin(value = "0.01") BigDecimal settledAmount,
        @NotBlank @Size(min = 3, max = 3) String settledAmountCurrency,
        @Size(max = 250) String message,
        @Size(max = 250) String unicodeEmoji,
        @Valid AccountDetails debtorAccountDetail,
        @Valid CreditorAccountDetail creditorAccountDetail
) {
}