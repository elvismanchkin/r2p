package dev.tsvinc.r2p;

public record R2PSettledEvent(R2PTransaction transaction, String agentId) {}
