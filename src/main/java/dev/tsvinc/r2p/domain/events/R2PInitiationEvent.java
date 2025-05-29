package dev.tsvinc.r2p.domain.events;

import dev.tsvinc.r2p.api.dto.request.InitiateR2pRequest;
import dev.tsvinc.r2p.domain.entity.R2PTransaction;

public record R2PInitiationEvent(R2PTransaction transaction, InitiateR2pRequest request) {}
