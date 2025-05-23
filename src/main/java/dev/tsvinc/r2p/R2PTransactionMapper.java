package dev.tsvinc.r2p;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class R2PTransactionMapper {
    private final ObjectMapper objectMapper;

    public R2PTransaction createTransactionFromInitiate(InitiateR2pRequest request, PaymentRequestDetail paymentRequest) {
        return R2PTransaction.builder()
                .paymentRequestId(UUID.randomUUID().toString().substring(0, 21)) // Generate Visa-style ID
                .endToEndId(paymentRequest.endToEndId())
                .requestMessageId(request.requestMessageId())
                .transactionStatus(TransactionStatus.PDNG.name())
                .useCase(request.useCase().name())
                .product(request.product().name())
                .requestedAmount(BigDecimal.valueOf(paymentRequest.requestedAmount()))
                .requestedAmountCurrency(paymentRequest.requestedAmountCurrency())
                .creditorAgentId(request.creditor().creditorAgentId())
                .debtorAgentId(paymentRequest.debtorAgentId())
                .creditorAlias(request.creditor().creditorAlias())
                .creditorAliasType(request.creditor().creditorAliasType() != null ?
                        request.creditor().creditorAliasType().name() : null)
                .debtorAlias(paymentRequest.debtorAlias())
                .debtorAliasType(paymentRequest.debtorAliasType() != null ?
                        paymentRequest.debtorAliasType().name() : null)
                .dueDate(request.dueDate() != null ? LocalDateTime.parse(request.dueDate() + "T23:59:59") : null)
                .requestReason(serializeToJson(request.requestReason()))
                .isRefund(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public R2PTransactionDto toDto(R2PTransaction transaction) {
        return new R2PTransactionDto(
                transaction.getPaymentRequestId(),
                transaction.getEndToEndId(),
                TransactionStatus.valueOf(transaction.getTransactionStatus()),
                UseCase.valueOf(transaction.getUseCase()),
                transaction.getRequestedAmount(),
                transaction.getRequestedAmountCurrency(),
                transaction.getAcceptedAmount(),
                transaction.getAcceptedAmountCurrency(),
                transaction.getCreditorAgentId(),
                transaction.getDebtorAgentId(),
                transaction.getDueDate(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }

    public String settlementDetailsToJson(SettlementDetails settlementDetails) {
        return serializeToJson(settlementDetails);
    }

    public String requestReasonToJson(RequestReason requestReason) {
        return serializeToJson(requestReason);
    }

    private String serializeToJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON: {}", object, e);
            return "{}";
        }
    }
}