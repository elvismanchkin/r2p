package dev.tsvinc.r2p.domain.events;

import dev.tsvinc.r2p.api.dto.request.AmendR2pRequest;
import dev.tsvinc.r2p.domain.entity.R2PTransaction;

public record R2PAmendmentEvent(R2PTransaction transaction, AmendR2pRequest request) {}
