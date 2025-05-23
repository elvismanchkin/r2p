package dev.tsvinc.r2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ConfirmR2pRequest(
        @NotBlank @Size(max = 35) String paymentRequestId,
        @NotBlank @Size(max = 35) String endToEndId,
        @NotBlank @Size(max = 35) String requestMessageId,
        @NotNull TransactionStatus transactionStatus,
        @Size(max = 4) String statusReason,
        @Size(max = 500) String message,
        @DecimalMin(value = "0.01") Double acceptedAmount,
        @Size(min = 3, max = 3 /*, exactly = 3*/) String acceptedAmountCurrency,
        @Valid SettlementDetails settlementDetails,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z") String creationDateTime) {}
