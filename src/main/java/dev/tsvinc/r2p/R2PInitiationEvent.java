package dev.tsvinc.r2p;

public record R2PInitiationEvent(R2PTransaction transaction, InitiateR2pRequest request) {}
