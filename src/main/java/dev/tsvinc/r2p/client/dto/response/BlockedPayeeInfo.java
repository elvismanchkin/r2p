package dev.tsvinc.r2p.client.dto.response;

import jakarta.validation.constraints.NotBlank;

public record BlockedPayeeInfo(
        @NotBlank String product,
        @NotBlank String blockReason,
        @NotBlank String referenceId,
        @NotBlank String blockCreatedDate,
        String creditorAgentId,
        String creditorAlias,
        String creditorAliasType,
        String creditorFirstName,
        String creditorLastName) {}
