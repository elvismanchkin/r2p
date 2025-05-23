package dev.tsvinc.r2p;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

// Error handling DTOs
public record ErrorResponse(
        @NotBlank @Size(max = 6) String code,
        @NotBlank String message,
        @NotBlank String creationDateTime,
        @NotBlank String requestMessageId,
        @NotBlank String responseMessageId,
        @Valid List<ErrorDetail> details
) {}