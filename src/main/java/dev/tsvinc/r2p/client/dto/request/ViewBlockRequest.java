package dev.tsvinc.r2p.client.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// View Block Request/Response
public record ViewBlockRequest(
        @NotBlank @Size(max = 35) String debtorId,
        @NotBlank String debtorIdType,
        @Size(max = 35) String debtorAgentId,
        @NotBlank @Size(max = 35) String requestMessageId,
        @NotBlank String creationDateTime) {}
