package dev.tsvinc.r2p;

import jakarta.validation.constraints.Size;

public record AccountDetails(
        @Size(min = 13, max = 19) String primaryAccountNumber, @Size(max = 35) String debtorTaxId) {}
