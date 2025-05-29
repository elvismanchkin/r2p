package dev.tsvinc.r2p.api.controller;

import dev.tsvinc.r2p.client.dto.request.ReferenceDataRequest;
import dev.tsvinc.r2p.client.dto.response.ReferenceDataResponse;
import dev.tsvinc.r2p.client.service.R2PWebClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/rtx/api/v1/requestToPay")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Request to Pay", description = "Reference Data APIs")
public class ReferenceDataController {

    private final R2PWebClientService webClientService;

    @Operation(summary = "Reference Data",
            description = "Retrieve reference data including available participants")
    @ApiResponse(responseCode = "200", description = "Reference data retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping(value = "/referenceData",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ReferenceDataResponse>> getReferenceData(
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("keyID") String keyId,
            @Valid @RequestBody ReferenceDataRequest request) {

        log.info("Retrieving reference data - types: {}", request.referenceDataTypes());

        return webClientService.getReferenceData(keyId, request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> {
                    if (response.getBody() != null) {
                        log.info("Retrieved reference data with {} participants",
                                response.getBody().availableParticipants().size());
                    }
                })
                .doOnError(error -> log.error("Failed to retrieve reference data: {}", error.getMessage()));
    }
}