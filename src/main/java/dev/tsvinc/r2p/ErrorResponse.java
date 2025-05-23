package dev.tsvinc.r2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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