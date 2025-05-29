package dev.tsvinc.r2p.api.controller;

import dev.tsvinc.r2p.api.dto.request.ConfirmR2pRequest;
import dev.tsvinc.r2p.api.dto.request.InitiateR2pRequest;
import dev.tsvinc.r2p.api.dto.response.ConfirmR2pResponse;
import dev.tsvinc.r2p.api.dto.response.InitiateR2pResponse;
import dev.tsvinc.r2p.client.dto.response.RetrieveR2pResponse;
import dev.tsvinc.r2p.client.service.R2PWebClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/r2p")
@RequiredArgsConstructor
public class RequestToPayInboundController {
    private final R2PWebClientService webClientService;

    @PostMapping("/initiate")
    public Mono<InitiateR2pResponse> initiateR2P(
            @RequestHeader String keyId,
            @Valid @RequestBody InitiateR2pRequest request) {

        String requestAffinity = UUID.randomUUID().toString();
        return webClientService.initiateR2P(keyId, requestAffinity, request);
    }

    @GetMapping("/{paymentRequestId}")
    public Mono<RetrieveR2pResponse> retrieveR2P(
            @PathVariable String paymentRequestId,
            @RequestHeader String keyId) {

        return webClientService.retrieveR2P(paymentRequestId, keyId, null);
    }

    @PatchMapping("/{paymentRequestId}/confirm")
    public Mono<ConfirmR2pResponse> confirmR2P(
            @PathVariable String paymentRequestId,
            @RequestHeader String keyId,
            @Valid @RequestBody ConfirmR2pRequest request) {

        return webClientService.confirmR2P(paymentRequestId, keyId, null, request);
    }
}
