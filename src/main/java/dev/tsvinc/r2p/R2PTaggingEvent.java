package dev.tsvinc.r2p;

public record R2PTaggingEvent(R2PTransaction transaction, TransactionTaggingRequest request) {}
