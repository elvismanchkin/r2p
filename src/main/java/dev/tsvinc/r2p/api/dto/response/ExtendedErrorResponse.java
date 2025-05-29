package dev.tsvinc.r2p.api.dto.response;

import dev.tsvinc.r2p.api.dto.request.ErrorDetail;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ExtendedErrorResponse(
        @NotBlank @Size(max = 6) @Pattern(regexp = "RC[1-5]\\d{3}") String code,
        @NotBlank String message,
        @NotBlank String creationDateTime,
        @NotBlank String requestMessageId,
        @NotBlank String responseMessageId,
        @Valid List<ErrorDetail> details
) {}