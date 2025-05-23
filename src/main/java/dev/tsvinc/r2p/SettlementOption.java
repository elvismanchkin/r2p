package dev.tsvinc.r2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SettlementOption(
        @NotNull SettlementSystem settlementSystem,
        @Size(min = 13, max = 19) String primaryAccountNumber,
        @Size(max = 35) String receivingAlias,
        AliasType receivingAliasType
) {
    public SettlementOption {
        // Validation logic
        if (settlementSystem == SettlementSystem.VISA_DIRECT ||
                settlementSystem == SettlementSystem.MASTERCARD) {
            if (primaryAccountNumber == null || primaryAccountNumber.isBlank()) {
                throw new IllegalArgumentException("PAN is required for " + settlementSystem);
            }
        }
        if (settlementSystem == SettlementSystem.DEFERRED_TO_ALIAS) {
            if (receivingAlias == null || receivingAliasType == null) {
                throw new IllegalArgumentException("Alias details required for DEFERRED_TO_ALIAS");
            }
        }
    }
}
