package dev.tsvinc.r2p;

public record R2PExpiredEvent(R2PTransaction transaction, String agentId) {}
