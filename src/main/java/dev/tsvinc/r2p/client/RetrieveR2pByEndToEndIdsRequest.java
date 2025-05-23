package dev.tsvinc.r2p.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

// Retrieve by End-to-End IDs
public record RetrieveR2pByEndToEndIdsRequest(
        @NotBlank @Size(max = 35) String agentId,
        @NotEmpty @Size(max = 10) List<@Size(max = 35) String> endToEndIds
) {
}
