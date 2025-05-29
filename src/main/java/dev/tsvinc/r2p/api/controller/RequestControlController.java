package dev.tsvinc.r2p.api.controller;

import dev.tsvinc.r2p.client.dto.request.ViewBlockRequest;
import dev.tsvinc.r2p.client.dto.response.ViewBlockResponse;
import dev.tsvinc.r2p.client.service.R2PWebClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/rtx/api/v1/requestControl")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Request Control", description = "Request Control APIs for managing blocks and access controls")
public class RequestControlController {

    private final R2PWebClientService webClientService;

    @Operation(summary = "Request Control View Block",
            description = "View active blocks for a debtor")
    @ApiResponse(responseCode = "200", description = "Active blocks retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping(value = "/view",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ViewBlockResponse>> viewBlocks(
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("keyID") String keyId,
            @Valid @RequestBody ViewBlockRequest request) {

        log.info("Viewing blocks for debtor: {}", request.debtorId());

        return webClientService.viewBlocks(keyId, request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> {
                    if (response.getBody() != null) {
                        log.info("Retrieved {} active blocks for debtor: {}",
                                response.getBody().blockedPayees().size(), request.debtorId());
                    }
                })
                .doOnError(error -> log.error("Failed to view blocks for debtor {}: {}",
                        request.debtorId(), error.getMessage()));
    }

    @Operation(summary = "Request Control Remove Block",
            description = "Remove a previously applied block")
    @ApiResponse(responseCode = "200", description = "Block removed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid block reference ID")
    @ApiResponse(responseCode = "404", description = "Block not found")
    @PatchMapping(value = "/{blockReferenceId}/remove")
    public Mono<ResponseEntity<Void>> removeBlock(
            @Parameter(description = "Block reference ID", required = true)
            @PathVariable String blockReferenceId,
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("keyID") String keyId) {

        log.info("Removing block: {}", blockReferenceId);

        return webClientService.removeBlock(blockReferenceId, keyId)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .doOnSuccess(v -> log.info("Block removed successfully: {}", blockReferenceId))
                .doOnError(error -> log.error("Failed to remove block {}: {}",
                        blockReferenceId, error.getMessage()));
    }
}