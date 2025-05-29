package dev.tsvinc.r2p.domain.events;

import dev.tsvinc.r2p.domain.entity.R2PTransaction;

public record R2PRejectedEvent(R2PTransaction transaction, String agentId) {}
