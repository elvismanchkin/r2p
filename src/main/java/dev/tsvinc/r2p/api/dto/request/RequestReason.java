package dev.tsvinc.r2p.api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RequestReason(
        @Size(max = 250) String message,
        @Size(max = 250) String unicodeEmoji,
        @Size(max = 12) String paymentPurpose,
        @Valid @Size(max = 10) List<ReferenceBlock> references
) {}