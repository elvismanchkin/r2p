package dev.tsvinc.r2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionTaggingRequest(
        @NotNull @Valid MessageEvent messageEvent,
        @NotNull @Valid TaggedTransaction taggedTransaction
) {}

