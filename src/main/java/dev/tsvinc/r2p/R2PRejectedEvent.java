package dev.tsvinc.r2p;

public record R2PRejectedEvent(R2PTransaction transaction, String agentId) {}
