package dev.tsvinc.r2p.domain.events;

import dev.tsvinc.r2p.api.dto.request.RefundR2pRequest;
import dev.tsvinc.r2p.domain.entity.R2PTransaction;

public record R2PRefundEvent(R2PTransaction transaction, RefundR2pRequest request) {}
