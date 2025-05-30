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
        @Valid List<NationalIdentifier> nationalIdentifiers
) {
    public Creditor {
        // Only P2P validations
        if (creditorFirstName == null || creditorLastName == null) {
            throw new IllegalArgumentException("First and last name required for P2P");
        }
    }
}
