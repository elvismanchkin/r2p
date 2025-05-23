package dev.tsvinc.r2p;

public record R2PRefundEvent(R2PTransaction transaction, RefundR2pRequest request) {
}
