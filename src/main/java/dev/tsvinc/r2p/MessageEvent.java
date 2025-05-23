package dev.tsvinc.r2p;

import jakarta.validation.constraints.Size;

public record MessageEvent(
        @Size(max = 250) String creditorAckEmoji,
        @Size(max = 250) String creditorAckMessage
) {
    public MessageEvent {
        if (creditorAckEmoji == null && creditorAckMessage == null) {
            throw new IllegalArgumentException("At least one acknowledgment field must be present");
        }
    }
}
