package dev.tsvinc.r2p.domain.events;

import dev.tsvinc.r2p.api.dto.request.ConfirmR2pRequest;
import dev.tsvinc.r2p.domain.entity.R2PTransaction;

// Event records
public record R2PConfirmationEvent(R2PTransaction transaction, ConfirmR2pRequest request) {}
