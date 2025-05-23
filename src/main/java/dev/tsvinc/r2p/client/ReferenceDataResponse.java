package dev.tsvinc.r2p.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReferenceDataResponse(
        List<ParticipantInfo> availableParticipants,
        @NotBlank @Size(max = 35) String requestMessageId,
        @NotBlank @Size(max = 35) String responseMessageId,
        @NotBlank String creationDateTime
) {
}
