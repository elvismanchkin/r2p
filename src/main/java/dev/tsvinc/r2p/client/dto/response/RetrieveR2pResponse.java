package dev.tsvinc.r2p.client.dto.response;

import dev.tsvinc.r2p.api.dto.request.Creditor;
import dev.tsvinc.r2p.api.dto.request.RequestOptions;
import dev.tsvinc.r2p.api.dto.request.RequestReason;
import dev.tsvinc.r2p.api.dto.request.SettlementDetails;
import dev.tsvinc.r2p.api.dto.request.SettlementOption;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

// Single Retrieve Response
public record RetrieveR2pResponse(
        @NotBlank String product,
        @NotBlank String useCase,
        @NotBlank String transactionStatus,
        @NotBlank String responseMessageId,
        @NotBlank String creationDateTime,
        @NotBlank String paymentRequestCreationDateTime,
        String dueDate,
        String message,
        String statusReason,
        RequestReason requestReason,
        PaymentRequestInitiated paymentRequest,
        RequestOptions requestOptions,
        Creditor creditor,
        List<SettlementDetails> settlementDetails,
        List<SettlementOption> settlementOptions
) {
}
