package dev.tsvinc.r2p.domain.repository;

import dev.tsvinc.r2p.domain.entity.R2PTransaction;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface R2PTransactionRepository extends R2dbcRepository<R2PTransaction, Long> {

    Mono<R2PTransaction> findByPaymentRequestId(String paymentRequestId);

    Mono<R2PTransaction> findByEndToEndId(String endToEndId);

    Flux<R2PTransaction> findByCreditorAgentId(String creditorAgentId);

    Flux<R2PTransaction> findByDebtorAgentId(String debtorAgentId);

    Flux<R2PTransaction> findByTransactionStatus(String status);

    @Query("SELECT * FROM r2p_transactions WHERE payment_request_id = ANY(:paymentRequestIds)")
    Flux<R2PTransaction> findByPaymentRequestIdIn(@Param("paymentRequestIds") String[] paymentRequestIds);

    @Query("SELECT * FROM r2p_transactions WHERE end_to_end_id = ANY(:endToEndIds)")
    Flux<R2PTransaction> findByEndToEndIdIn(@Param("endToEndIds") String[] endToEndIds);

    @Query("SELECT * FROM r2p_transactions WHERE creditor_agent_id = :agentId OR debtor_agent_id = :agentId")
    Flux<R2PTransaction> findByAgentId(@Param("agentId") String agentId);

    @Query("SELECT * FROM r2p_transactions WHERE transaction_status = 'PDNG' AND due_date < :now")
    Flux<R2PTransaction> findExpiredTransactions(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE r2p_transactions SET transaction_status = :status, updated_at = NOW() WHERE payment_request_id = :paymentRequestId")
    Mono<Integer> updateTransactionStatus(@Param("paymentRequestId") String paymentRequestId,
                                          @Param("status") String status);

    @Modifying
    @Query("UPDATE r2p_transactions SET transaction_status = :status, accepted_amount = :acceptedAmount, " +
            "accepted_amount_currency = :acceptedAmountCurrency, settlement_details_json = :settlementDetails, " +
            "updated_at = NOW() WHERE payment_request_id = :paymentRequestId")
    Mono<Integer> updateTransactionWithSettlement(@Param("paymentRequestId") String paymentRequestId,
                                                  @Param("status") String status,
                                                  @Param("acceptedAmount") BigDecimal acceptedAmount,
                                                  @Param("acceptedAmountCurrency") String acceptedAmountCurrency,
                                                  @Param("settlementDetails") String settlementDetails);

    @Query("SELECT * FROM r2p_transactions WHERE is_refund = true AND original_payment_request_id = :originalId")
    Flux<R2PTransaction> findRefundsByOriginalPaymentRequestId(@Param("originalId") String originalId);

    @Query("SELECT * FROM r2p_transactions WHERE created_at BETWEEN :startDate AND :endDate")
    Flux<R2PTransaction> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);
}