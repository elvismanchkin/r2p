package dev.tsvinc.r2p;

public record R2PCancellationEvent(R2PTransaction transaction, CancelR2pRequest request) {
}
