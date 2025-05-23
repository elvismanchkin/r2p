package dev.tsvinc.r2p;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class R2PTransactionMapper {
    private final ObjectMapper objectMapper;

    public R2PTransaction createTransactionFromInitiate(InitiateR2pRequest request, InitiateR2pResponse response) {
        PaymentRequestDetail requestDetail = request.paymentRequests().get(0);
        PaymentRequestMinResponse responseDetail = response.paymentRequests().get(0);

        return R2PTransaction.builder()
                .paymentRequestId(responseDetail.paymentRequestId())
                .endToEndId(requestDetail.endToEndId())
                .requestMessageId(request.requestMessageId())
                .responseMessageId(response.responseMessageId())
                .transactionStatus(responseDetail.transactionStatus().name())
                .useCase(request.useCase().name())
                .product(request.product().name())
                .requestedAmount(BigDecimal.valueOf(requestDetail.requestedAmount()))
                .requestedAmountCurrency(requestDetail.requestedAmountCurrency())
                .creditorAgentId(request.creditor().creditorAgentId())
                .debtorAgentId(requestDetail.debtorAgentId())
                .creditorAlias(request.creditor().creditorAlias())
                .creditorAliasType(request.creditor().creditorAliasType().name())
                .debtorAlias(requestDetail.debtorAlias())
                .debtorAliasType(requestDetail.debtorAliasType().name())
                .dueDate(request.dueDate() != null ? LocalDateTime.parse(request.dueDate() + "T00:00:00") : null)
                .requestReason(serializeToJson(request.requestReason()))
                .isRefund(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public R2PTransaction createRefundTransaction(String originalPaymentRequestId, RefundR2pRequest request, RefundR2pResponse response) {
        RefundPaymentRequest refundRequest = request.paymentRequests().get(0);
        PaymentRequestMinResponse responseDetail = response.paymentRequests().get(0);

        return R2PTransaction.builder()
                .paymentRequestId(responseDetail.paymentRequestId())
                .endToEndId(refundRequest.endToEndId())
                .requestMessageId(request.requestMessageId())
                .responseMessageId(response.responseMessageId())
                .transactionStatus(responseDetail.transactionStatus().name())
                .useCase("B2C") // Refunds are typically B2C
                .product("VD")  // Using VD as default product
                .requestedAmount(BigDecimal.valueOf(refundRequest.requestedAmount()))
                .creditorAgentId(request.creditor().creditorAgentId())
                .originalPaymentRequestId(originalPaymentRequestId)
                .isRefund(true)
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

    public ConfirmR2pResponse createConfirmResponse(String paymentRequestId, ConfirmR2pRequest request) {
        return new ConfirmR2pResponse(
                UUID.randomUUID().toString(),
                paymentRequestId,
                request.endToEndId(),
                request.requestMessageId(),
                request.transactionStatus(),
                Instant.now().toString()
        );
    }

    private String serializeToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize to JSON", e);
            return "{}";
        }
    }
}
