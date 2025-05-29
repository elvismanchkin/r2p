package dev.tsvinc.r2p.service;

import dev.tsvinc.r2p.domain.entity.R2PTransaction;
import dev.tsvinc.r2p.domain.repository.R2PTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class R2PTransactionService {

    private final R2PTransactionRepository transactionRepository;

    public Mono<R2PTransaction> findById(Long id) {
        return transactionRepository.findById(id)
                .doOnSuccess(transaction -> {
                    if (transaction != null) {
                        log.debug("Found transaction with ID: {}", id);
                    } else {
                        log.debug("No transaction found with ID: {}", id);
                    }
                })
                .doOnError(error -> log.error("Error finding transaction with ID: {}", id, error));
    }

    public Mono<R2PTransaction> findByPaymentRequestId(String paymentRequestId) {
        return transactionRepository.findByPaymentRequestId(paymentRequestId)
                .doOnSuccess(transaction -> {
                    if (transaction != null) {
                        log.debug("Found transaction with payment request ID: {}", paymentRequestId);
                    } else {
                        log.debug("No transaction found with payment request ID: {}", paymentRequestId);
                    }
                })
                .doOnError(error -> log.error("Error finding transaction with payment request ID: {}", paymentRequestId, error));
    }

    public Mono<R2PTransaction> save(R2PTransaction transaction) {
        if (transaction.getId() == null) {
            transaction.setCreatedAt(LocalDateTime.now());
        }
        transaction.setUpdatedAt(LocalDateTime.now());

        return transactionRepository.save(transaction)
                .doOnSuccess(saved -> log.debug("Saved transaction with payment request ID: {}", saved.getPaymentRequestId()))
                .doOnError(error -> log.error("Error saving transaction: {}", transaction, error));
    }

    @Transactional
    public Mono<R2PTransaction> updateTransactionStatus(String paymentRequestId, String status) {
        return transactionRepository.updateTransactionStatus(paymentRequestId, status)
                .then(transactionRepository.findByPaymentRequestId(paymentRequestId))
                .doOnSuccess(transaction -> log.debug("Updated transaction status to {} for payment request ID: {}",
                        status, paymentRequestId))
                .doOnError(error -> log.error("Error updating transaction status for payment request ID: {}",
                        paymentRequestId, error));
    }

    @Transactional
    public Mono<R2PTransaction> updateTransactionWithSettlement(
            String paymentRequestId,
            String status,
            BigDecimal acceptedAmount,
            String acceptedAmountCurrency,
            String settlementDetails) {

        return transactionRepository.updateTransactionWithSettlement(
                        paymentRequestId, status, acceptedAmount, acceptedAmountCurrency, settlementDetails)
                .then(transactionRepository.findByPaymentRequestId(paymentRequestId))
                .doOnSuccess(transaction -> log.debug("Updated transaction with settlement details for payment request ID: {}",
                        paymentRequestId))
                .doOnError(error -> log.error("Error updating transaction with settlement for payment request ID: {}",
                        paymentRequestId, error));
    }

    public Flux<R2PTransaction> findByTransactionStatus(String status) {
        return transactionRepository.findByTransactionStatus(status)
                .doOnComplete(() -> log.debug("Retrieved transactions with status: {}", status))
                .doOnError(error -> log.error("Error finding transactions with status: {}", status, error));
    }

    public Flux<R2PTransaction> findExpiredTransactions() {
        return transactionRepository.findExpiredTransactions(LocalDateTime.now())
                .doOnComplete(() -> log.debug("Retrieved expired transactions"))
                .doOnError(error -> log.error("Error finding expired transactions", error));
    }

    public Flux<R2PTransaction> findRefundsByOriginalPaymentRequestId(String originalId) {
        return transactionRepository.findRefundsByOriginalPaymentRequestId(originalId)
                .doOnComplete(() -> log.debug("Retrieved refunds for original payment request ID: {}", originalId))
                .doOnError(error -> log.error("Error finding refunds for original payment request ID: {}", originalId, error));
    }

    public Mono<Void> deleteById(Long id) {
        return transactionRepository.deleteById(id)
                .doOnSuccess(unused -> log.debug("Deleted transaction with ID: {}", id))
                .doOnError(error -> log.error("Error deleting transaction with ID: {}", id, error));
    }
}