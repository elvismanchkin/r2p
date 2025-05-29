package dev.tsvinc.r2p.api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.tsvinc.r2p.domain.enums.AliasType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Creditor(
        @NotBlank @Size(max = 35) String creditorAgentId,
        @NotBlank @Size(min = 2, max = 2) String creditorCountry,
        @NotBlank @Size(min = 2, max = 2) String creditorAgentCountry,
        @Size(max = 35) String creditorAlias,
        AliasType creditorAliasType,
        @Size(max = 140) String creditorFirstName,
        @Size(max = 140) @Pattern(regexp = "^[A-Z]\\.?$") String creditorLastName,
        @Size(max = 140) String creditorBusinessName,
        @Size(min = 3, max = 4) String creditorMcc,
        @Size(max = 20) String creditorTaxId,
        @Valid List<NationalIdentifier> nationalIdentifiers
) {
    public Creditor {
        // Validate based on use case
        if (creditorBusinessName != null && !creditorBusinessName.isBlank()) {
            // B2C validations
            if (creditorMcc == null || creditorMcc.isBlank()) {
                throw new IllegalArgumentException("MCC is required for B2C");
            }
        } else {
            // P2P validations
            if (creditorFirstName == null || creditorLastName == null) {
                throw new IllegalArgumentException("First and last name required for P2P");
            }
        }
    }
}
