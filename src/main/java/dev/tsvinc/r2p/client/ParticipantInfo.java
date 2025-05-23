package dev.tsvinc.r2p.client;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ParticipantInfo(
        @NotBlank String participantId,
        @NotBlank String participantName,
        List<String> supportedFeatures
) {
}
