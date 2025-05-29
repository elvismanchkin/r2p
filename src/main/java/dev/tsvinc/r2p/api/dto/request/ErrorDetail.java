package dev.tsvinc.r2p.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ErrorDetail(@NotBlank String field, @NotBlank String reason) {}
