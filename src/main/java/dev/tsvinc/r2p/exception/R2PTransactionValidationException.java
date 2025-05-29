package dev.tsvinc.r2p.exception;

public class R2PTransactionValidationException extends RuntimeException {
    public R2PTransactionValidationException(String message) {
        super(message);
    }
}