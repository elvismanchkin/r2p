package dev.tsvinc.r2p.service;

import dev.tsvinc.r2p.api.dto.request.AmendR2pRequest;
import dev.tsvinc.r2p.api.dto.request.CancelR2pRequest;
import dev.tsvinc.r2p.api.dto.request.ConfirmR2pRequest;
import dev.tsvinc.r2p.api.dto.request.InitiateR2pRequest;
import dev.tsvinc.r2p.api.dto.request.RefundR2pRequest;
import dev.tsvinc.r2p.api.dto.request.TransactionTaggingRequest;
import dev.tsvinc.r2p.domain.entity.R2PTransaction;
import dev.tsvinc.r2p.domain.events.R2PAmendmentEvent;
import dev.tsvinc.r2p.domain.events.R2PCancellationEvent;
import dev.tsvinc.r2p.domain.events.R2PConfirmationEvent;
import dev.tsvinc.r2p.domain.events.R2PExpiredEvent;
import dev.tsvinc.r2p.domain.events.R2PInitiationEvent;
import dev.tsvinc.r2p.domain.events.R2PRefundEvent;
import dev.tsvinc.r2p.domain.events.R2PRejectedEvent;
import dev.tsvinc.r2p.domain.events.R2PReminderEvent;
import dev.tsvinc.r2p.domain.events.R2PSettledEvent;
import dev.tsvinc.r2p.domain.events.R2PTaggingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
@Slf4j
public class R2PEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishConfirmationEvent(R2PTransaction transaction, ConfirmR2pRequest request) {
        publishEvent(new R2PConfirmationEvent(transaction, request));
    }

    public void publishInitiationEvent(R2PTransaction transaction, InitiateR2pRequest request) {
        publishEvent(new R2PInitiationEvent(transaction, request));
    }

    public void publishTaggingEvent(R2PTransaction transaction, TransactionTaggingRequest request) {
        publishEvent(new R2PTaggingEvent(transaction, request));
    }

    public void publishRefundEvent(R2PTransaction transaction, RefundR2pRequest request) {
        publishEvent(new R2PRefundEvent(transaction, request));
    }

    public void publishCancellationEvent(R2PTransaction transaction, CancelR2pRequest request) {
        publishEvent(new R2PCancellationEvent(transaction, request));
    }

    public void publishAmendmentEvent(R2PTransaction transaction, AmendR2pRequest request) {
        publishEvent(new R2PAmendmentEvent(transaction, request));
    }

    public Mono<Void> publishReminderEvent(R2PTransaction transaction, String agentId) {
        return Mono.fromRunnable(() -> publishEvent(new R2PReminderEvent(transaction, agentId)))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    public Mono<Void> publishExpiredEvent(R2PTransaction transaction, String agentId) {
        return Mono.fromRunnable(() -> publishEvent(new R2PExpiredEvent(transaction, agentId)))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    public Mono<Void> publishRejectedEvent(R2PTransaction transaction, String agentId) {
        return Mono.fromRunnable(() -> publishEvent(new R2PRejectedEvent(transaction, agentId)))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    public Mono<Void> publishSettledEvent(R2PTransaction transaction, String agentId) {
        return Mono.fromRunnable(() -> publishEvent(new R2PSettledEvent(transaction, agentId)))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private void publishEvent(Object event) {
        try {
            eventPublisher.publishEvent(event);
            log.debug("Published event: {}", event.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event.getClass().getSimpleName(), e);
        }
    }
}