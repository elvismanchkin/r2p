package dev.tsvinc.r2p.exception;

public class R2PTransactionNotFoundException extends RuntimeException {
    public R2PTransactionNotFoundException(String message) {
        super(message);
    }
}