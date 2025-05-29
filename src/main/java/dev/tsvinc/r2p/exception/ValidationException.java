package dev.tsvinc.r2p.exception;

// Validation exception
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
