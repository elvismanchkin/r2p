package dev.tsvinc.r2p.api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationR2pRequest(
        @NotBlank @Size(max = 35) String agentId,
        @NotEmpty @Valid List<ReminderEvent> events,
        @NotBlank @Size(max = 35) String requestMessageId,
        @NotBlank String creationDateTime
) {
}
