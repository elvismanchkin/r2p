package dev.tsvinc.r2p.api.dto.request;

import dev.tsvinc.r2p.domain.enums.AliasType;
import jakarta.validation.constraints.Size;

public record CreditorAccountDetail(
        @Size(min = 13, max = 19) String primaryAccountNumber,
        @Size(max = 35) String receivingAlias,
        AliasType receivingAliasType) {}
