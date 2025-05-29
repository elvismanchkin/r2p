package dev.tsvinc.r2p.service;

import dev.tsvinc.r2p.client.dto.request.ReferenceDataRequest;
import dev.tsvinc.r2p.client.dto.request.ViewBlockRequest;
import dev.tsvinc.r2p.client.dto.response.ReferenceDataResponse;
import dev.tsvinc.r2p.client.dto.response.ViewBlockResponse;
import dev.tsvinc.r2p.client.service.R2PWebClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferenceDataServiceImpl implements ReferenceDataService {

    private final R2PWebClientService webClientService;

    @Override
    public Mono<ReferenceDataResponse> getReferenceData(String keyId, ReferenceDataRequest request) {
        log.debug("Getting reference data for types: {}", request.referenceDataTypes());
        return webClientService.getReferenceData(keyId, request)
                .doOnSuccess(response -> log.info("Retrieved {} participants",
                        response.availableParticipants().size()))
                .doOnError(error -> log.error("Failed to get reference data: {}", error.getMessage()));
    }

    @Override
    public Mono<ViewBlockResponse> viewBlocks(String keyId, ViewBlockRequest request) {
        log.debug("Viewing blocks for debtor: {}", request.debtorId());
        return webClientService.viewBlocks(keyId, request)
                .doOnSuccess(response -> log.info("Retrieved {} blocks for debtor: {}",
                        response.blockedPayees().size(), request.debtorId()))
                .doOnError(error -> log.error("Failed to view blocks: {}", error.getMessage()));
    }

    @Override
    public Mono<Void> removeBlock(String blockReferenceId, String keyId) {
        log.debug("Removing block: {}", blockReferenceId);
        return webClientService.removeBlock(blockReferenceId, keyId)
                .doOnSuccess(v -> log.info("Block removed: {}", blockReferenceId))
                .doOnError(error -> log.error("Failed to remove block: {}", error.getMessage()));
    }
}