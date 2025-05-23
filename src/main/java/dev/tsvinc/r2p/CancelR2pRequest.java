package dev.tsvinc.r2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CancelR2pRequest(
        @NotBlank @Size(max = 35) String requestMessageId,
        @NotBlank @Pattern(regexp = "ERRAMT|ERRDRINFO|PAID|DRTP|ERR|SVNR|FRAUD|OTHR") String cancellationReason,
        @NotBlank String creationDateTime) {}
