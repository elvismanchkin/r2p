package dev.tsvinc.r2p;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestToPayOutboundService {

    private final R2PTransactionRepository transactionRepository;
    private final R2PTransactionMapper transactionMapper;
    private final R2PEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    private Counter confirmationCounter;
    private Counter initiationCounter;
    private Counter cancellationCounter;
    private Counter refundCounter;

    @PostConstruct
    public void init() {
        confirmationCounter = Counter.builder("r2p.outbound.confirmations")
                .description("Number of R2P confirmations processed")
                .register(meterRegistry);

        initiationCounter = Counter.builder("r2p.outbound.initiations")
                .description("Number of R2P initiations processed")
                .register(meterRegistry);

        cancellationCounter = Counter.builder("r2p.outbound.cancellations")
                .description("Number of R2P cancellations processed")
                .register(meterRegistry);

        refundCounter = Counter.builder("r2p.outbound.refunds")
                .description("Number of R2P refunds processed")
                .register(meterRegistry);
    }

    @Transactional
    public Mono<ConfirmR2pResponse> processConfirmation(String paymentRequestId, String keyId,
                                                        String requestAffinity, ConfirmR2pRequest request) {
        return transactionRepository.findByPaymentRequestId(paymentRequestId)
                .switchIfEmpty(Mono.error(new R2PNotFoundException("Payment request not found: " + paymentRequestId)))
                .filter(transaction -> transaction.getEndToEndId().equals(request.endToEndId()))
                .switchIfEmpty(Mono.error(new R2PBusinessException("End-to-end ID mismatch")))
                .flatMap(transaction -> updateTransactionForConfirmation(transaction, request))
                .flatMap(transactionRepository::save)
                .doOnNext(transaction -> {
                    confirmationCounter.increment();
                    eventPublisher.publishConfirmationEvent(transaction, request);
                })
                .map(transaction -> ConfirmR2pResponse.create(request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> processTransactionTagging(String keyId, String requestAffinity,
                                                TransactionTaggingRequest request) {
        return transactionRepository.findByPaymentRequestId(request.taggedTransaction().transactionId())
                .switchIfEmpty(Mono.error(new R2PNotFoundException("Transaction not found: " +
                        request.taggedTransaction().transactionId())))
                .flatMap(transaction -> updateTransactionWithTagging(transaction, request))
                .flatMap(transactionRepository::save)
                .doOnNext(transaction -> eventPublisher.publishTaggingEvent(transaction, request))
                .then()
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional
    public Mono<RefundR2pResponse> processRefund(String originalPaymentRequestId, String keyId,
                                                 String requestAffinity, RefundR2pRequest request) {
        return transactionRepository.findByPaymentRequestId(originalPaymentRequestId)
                .switchIfEmpty(Mono.error(new R2PNotFoundException("Original payment request not found: " +
                        originalPaymentRequestId)))
                .filter(transaction -> TransactionStatus.ACSC.equals(transaction.getTransactionStatus()))
                .switchIfEmpty(Mono.error(new R2PBusinessException("Can only refund settled transactions")))
                .flatMap(originalTransaction -> createRefundTransaction(originalTransaction, request))
                .flatMap(transactionRepository::save)
                .doOnNext(transaction -> {
                    refundCounter.increment();
                    eventPublisher.publishRefundEvent(transaction, request);
                })
                .map(transaction -> RefundR2pResponse.create(request,
                        List.of(new PaymentRequestMinResponse(
                                transaction.getPaymentRequestId(),
                                transaction.getEndToEndId(),
                                TransactionStatus.PDNG,
                                null,
                                null
                        ))));
    }

    @Transactional
    public Mono<CancelR2pResponse> processCancellation(String paymentRequestId, String keyId,
                                                       String requestAffinity, CancelR2pRequest request) {
        return transactionRepository.findByPaymentRequestId(paymentRequestId)
                .switchIfEmpty(Mono.error(new R2PNotFoundException("Payment request not found: " + paymentRequestId)))
                .filter(transaction -> !isTerminalStatus(TransactionStatus.valueOf(transaction.getTransactionStatus())))
                .switchIfEmpty(Mono.error(new R2PBusinessException("Cannot cancel transaction in terminal status")))
                .flatMap(transaction -> updateTransactionForCancellation(transaction, request))
                .flatMap(transactionRepository::save)
                .doOnNext(transaction -> {
                    cancellationCounter.increment();
                    eventPublisher.publishCancellationEvent(transaction, request);
                })
                .map(transaction -> CancelR2pResponse.create(request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional
    public Mono<InitiateR2pResponse> processInitiation(String keyId, String requestAffinity,
                                                       InitiateR2pRequest request) {
        return Flux.fromIterable(request.paymentRequests())
                .flatMap(paymentRequest -> createTransactionFromInitiate(request, paymentRequest))
                .flatMap(transactionRepository::save)
                .doOnNext(transaction -> {
                    initiationCounter.increment();
                    eventPublisher.publishInitiationEvent(transaction, request);
                })
                .map(transaction -> new PaymentRequestMinResponse(
                        transaction.getPaymentRequestId(),
                        transaction.getEndToEndId(),
                        TransactionStatus.PDNG,
                        transaction.getDebtorAlias(),
                        transaction.getDebtorAliasType()
                ))
                .collectList()
                .map(paymentResponses -> new InitiateR2pResponse(
                        UUID.randomUUID().toString(),
                        paymentResponses,
                        request.requestMessageId(),
                        Instant.now().toString()
                ))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional
    public Mono<AmendR2pResponse> processAmendment(String paymentRequestId, String keyId,
                                                   String requestAffinity, AmendR2pRequest request) {
        return transactionRepository.findByPaymentRequestId(paymentRequestId)
                .switchIfEmpty(Mono.error(new R2PNotFoundException("Payment request not found: " + paymentRequestId)))
                .filter(transaction -> !isTerminalStatus(TransactionStatus.valueOf(transaction.getTransactionStatus())))
                .switchIfEmpty(Mono.error(new R2PBusinessException("Cannot amend transaction in terminal status")))
                .flatMap(transaction -> updateTransactionForAmendment(transaction, request))
                .flatMap(transactionRepository::save)
                .doOnNext(transaction -> eventPublisher.publishAmendmentEvent(transaction, request))
                .map(transaction -> AmendR2pResponse.create(paymentRequestId, request,
                        TransactionStatus.valueOf(transaction.getTransactionStatus())))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> processNotifications(String keyId, NotificationR2pRequest request) {
        return Flux.fromIterable(request.events())
                .flatMap(event -> processNotificationEvent(event, request.agentId()))
                .then()
                .subscribeOn(Schedulers.boundedElastic());
    }

    // Private helper methods
    private Mono<R2PTransaction> updateTransactionForConfirmation(R2PTransaction transaction,
                                                                  ConfirmR2pRequest request) {
        return Mono.fromCallable(() -> {
            transaction.setTransactionStatus(request.transactionStatus().name());
            transaction.setTransactionStatus(request.statusReason());
            transaction.setMessage(request.message());

            if (request.acceptedAmount() != null) {
                transaction.setAcceptedAmount(BigDecimal.valueOf(request.acceptedAmount()));
                transaction.setAcceptedAmountCurrency(request.acceptedAmountCurrency());
            }

            if (request.settlementDetails() != null) {
                transaction.setSettlementDetailsJson(
                        transactionMapper.settlementDetailsToJson(request.settlementDetails())
                );
            }

            return transaction;
        });
    }

    private Mono<R2PTransaction> updateTransactionWithTagging(R2PTransaction transaction,
                                                              TransactionTaggingRequest request) {
        return Mono.fromCallable(() -> {
            // Store tagging information
            transaction.setCreditorAckMessage(request.messageEvent().creditorAckMessage());
            transaction.setCreditorAckEmoji(request.messageEvent().creditorAckEmoji());
            return transaction;
        });
    }

    private Mono<R2PTransaction> createRefundTransaction(R2PTransaction originalTransaction,
                                                         RefundR2pRequest request) {
        return Mono.fromCallable(() -> {
            RefundPaymentRequest refundRequest = request.paymentRequests().get(0);

            R2PTransaction refundTransaction = new R2PTransaction();
            refundTransaction.setPaymentRequestId("RFD" + UUID.randomUUID().toString().substring(0, 18));
            refundTransaction.setEndToEndId(refundRequest.endToEndId());
            refundTransaction.setRequestMessageId(request.requestMessageId());
            refundTransaction.setTransactionStatus(TransactionStatus.PDNG);
            refundTransaction.setUseCase(UseCase.B2C);
            refundTransaction.setRequestedAmount(refundRequest.requestedAmount());
            refundTransaction.setRequestedAmountCurrency(originalTransaction.getRequestedAmountCurrency());
            refundTransaction.setOriginalPaymentRequestId(originalTransaction.getPaymentRequestId());
            refundTransaction.setPaymentRequestType("REFUND");

            // Copy relevant fields from original transaction
            refundTransaction.setCreditorAgentId(originalTransaction.getDebtorAgentId());
            refundTransaction.setDebtorAgentId(originalTransaction.getCreditorAgentId());

            return refundTransaction;
        });
    }

    private Mono<R2PTransaction> updateTransactionForCancellation(R2PTransaction transaction,
                                                                  CancelR2pRequest request) {
        return Mono.fromCallable(() -> {
            transaction.setTransactionStatus(TransactionStatus.CNCL);
            transaction.setCancellationReason(request.cancellationReason());
            return transaction;
        });
    }

    private Mono<R2PTransaction> createTransactionFromInitiate(InitiateR2pRequest request,
                                                               PaymentRequestDetail paymentRequest) {
        return Mono.fromCallable(() ->
                transactionMapper.createTransactionFromInitiate(request, paymentRequest)
        );
    }

    private Mono<R2PTransaction> updateTransactionForAmendment(R2PTransaction transaction,
                                                               AmendR2pRequest request) {
        return Mono.fromCallable(() -> {
            if (request.dueDate() != null) {
                transaction.setDueDate(request.dueDate());
            }

            if (request.paymentRequest() != null && request.paymentRequest().requestedAmount() != null) {
                transaction.setRequestedAmount(request.paymentRequest().requestedAmount());
            }

            if (request.requestReason() != null) {
                transaction.setRequestReasonJson(
                        transactionMapper.requestReasonToJson(request.requestReason())
                );
            }

            return transaction;
        });
    }

    private Mono<Void> processNotificationEvent(ReminderEvent event, String agentId) {
        return transactionRepository.findByPaymentRequestId(event.paymentRequestId())
                .flatMap(transaction -> {
                    switch (event.eventType()) {
                        case "REMINDER" -> {
                            return eventPublisher.publishReminderEvent(transaction, agentId)
                                    .then();
                        }
                        case "EXPIRED" -> {
                            transaction.setTransactionStatus(TransactionStatus.EXPD);
                            return transactionRepository.save(transaction)
                                    .then(eventPublisher.publishExpiredEvent(transaction, agentId))
                                    .then();
                        }
                        case "REJECTED" -> {
                            transaction.setTransactionStatus(TransactionStatus.RJCT);
                            return transactionRepository.save(transaction)
                                    .then(eventPublisher.publishRejectedEvent(transaction, agentId))
                                    .then();
                        }
                        case "SETTLED" -> {
                            transaction.setTransactionStatus(TransactionStatus.ACSC);
                            return transactionRepository.save(transaction)
                                    .then(eventPublisher.publishSettledEvent(transaction, agentId))
                                    .then();
                        }
                        default -> {
                            return Mono.empty();
                        }
                    }
                })
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
                .onErrorResume(error -> {
                    log.error("Failed to process notification event for paymentRequestId: {}",
                            event.paymentRequestId(), error);
                    return Mono.empty();
                });
    }

    private boolean isTerminalStatus(TransactionStatus status) {
        return TransactionStatus.ACSC.equals(status) ||
                TransactionStatus.RJCT.equals(status) ||
                TransactionStatus.CNCL.equals(status) ||
                TransactionStatus.EXPD.equals(status);
    }
}