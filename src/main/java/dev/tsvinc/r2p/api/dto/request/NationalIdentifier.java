package dev.tsvinc.r2p.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NationalIdentifier(
        @NotBlank @Pattern(regexp = "PASSPORT") String type, @NotBlank @Size(max = 50) String value) {}
