package dev.tsvinc.r2p.domain.events;

import dev.tsvinc.r2p.api.dto.request.CancelR2pRequest;
import dev.tsvinc.r2p.domain.entity.R2PTransaction;

public record R2PCancellationEvent(R2PTransaction transaction, CancelR2pRequest request) {}
