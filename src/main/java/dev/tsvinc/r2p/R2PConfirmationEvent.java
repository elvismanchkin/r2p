package dev.tsvinc.r2p;

// Event records
public record R2PConfirmationEvent(R2PTransaction transaction, ConfirmR2pRequest request) {
}
