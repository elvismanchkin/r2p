package dev.tsvinc.r2p;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TaggedTransaction(
        @NotBlank @Size(max = 100) String transactionId,
        @NotBlank @Pattern(regexp = "R2P") String transactionIdType,
        @Size(max = 36) String settlementSystemReferenceId,
        String settlementSystemReferenceType
) {
}
