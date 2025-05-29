package dev.tsvinc.r2p.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReminderEvent(
        @NotBlank @Size(max = 35) String paymentRequestId,
        @NotBlank @Pattern(regexp = "REMINDER|EXPIRED|REJECTED|SETTLED") String eventType) {}
