package dev.tsvinc.r2p.client.dto.response;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ParticipantInfo(
        @NotBlank String participantId,
        @NotBlank String participantName,
        List<String> supportedFeatures
) {
}
