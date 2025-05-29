package dev.tsvinc.r2p.service;

import dev.tsvinc.r2p.client.dto.request.ReferenceDataRequest;
import dev.tsvinc.r2p.client.dto.request.ViewBlockRequest;
import dev.tsvinc.r2p.client.dto.response.ReferenceDataResponse;
import dev.tsvinc.r2p.client.dto.response.ViewBlockResponse;
import reactor.core.publisher.Mono;

public interface ReferenceDataService {

    Mono<ReferenceDataResponse> getReferenceData(String keyId, ReferenceDataRequest request);

    Mono<ViewBlockResponse> viewBlocks(String keyId, ViewBlockRequest request);

    Mono<Void> removeBlock(String blockReferenceId, String keyId);
}