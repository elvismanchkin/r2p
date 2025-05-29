package dev.tsvinc.r2p.exception;

public class R2PTransactionProcessingException extends RuntimeException {
    public R2PTransactionProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}