package dev.tsvinc.r2p.client.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ViewBlockResponse(
        @NotEmpty List<BlockedPayeeInfo> blockedPayees,
        @NotBlank @Size(max = 35) String requestMessageId,
        @NotBlank @Size(max = 35) String responseMessageId,
        @NotBlank String creationDateTime
) {
}
