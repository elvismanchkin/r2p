package dev.tsvinc.r2p;

// Validation exception
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
