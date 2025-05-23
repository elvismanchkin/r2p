package dev.tsvinc.r2p;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("r2p_transactions")
public class R2PTransaction {

    @Id
    private Long id;

    @Column("payment_request_id")
    private String paymentRequestId;

    @Column("end_to_end_id")
    private String endToEndId;

    @Column("request_message_id")
    private String requestMessageId;

    @Column("response_message_id")
    private String responseMessageId;

    @Column("transaction_status")
    private TransactionStatus transactionStatus;

    @Column("cancellation_reason")
    private String cancellationReason;

    @Column("use_case")
    private String useCase;

    @Column("product")
    private String product;

    @Column("requested_amount")
    private BigDecimal requestedAmount;

    @Column("requested_amount_currency")
    private String requestedAmountCurrency;

    @Column("accepted_amount")
    private BigDecimal acceptedAmount;

    @Column("accepted_amount_currency")
    private String acceptedAmountCurrency;

    @Column("creditor_agent_id")
    private String creditorAgentId;

    @Column("debtor_agent_id")
    private String debtorAgentId;

    @Column("creditor_alias")
    private String creditorAlias;

    @Column("debtor_alias")
    private String debtorAlias;

    @Column("creditor_alias_type")
    private String creditorAliasType;

    @Column("debtor_alias_type")
    private String debtorAliasType;

    @Column("due_date")
    private LocalDate dueDate;

    @Column("request_reason")
    private String requestReason;

    @Column("message")
    private String message;

    @Column("settlement_details_json")
    private String settlementDetailsJson;

    @Column("original_payment_request_id")
    private String originalPaymentRequestId;

    @Column("is_refund")
    private Boolean isRefund;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("creditor_ack_message")
    private String creditorAckMessage;

    @Column("creditor_ack_emoji")
    private String creditorAckEmoji;

    @Column("payment_request_type")
    private String paymentRequestType;

    @Version
    private Long version;
}