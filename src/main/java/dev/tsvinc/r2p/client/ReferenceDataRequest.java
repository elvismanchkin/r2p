package dev.tsvinc.r2p.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

// Reference Data Request/Response
public record ReferenceDataRequest(
        @NotEmpty List<String> referenceDataTypes,
        @NotBlank @Size(max = 35) String requestMessageId,
        @NotBlank String creationDateTime
) {
}
