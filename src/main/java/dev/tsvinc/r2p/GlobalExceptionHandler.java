package dev.tsvinc.r2p;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(WebExchangeBindException ex) {
        List<ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::createErrorDetail)
                .toList();

        ErrorResponse errorResponse = new ErrorResponse(
                "RC1000",
                "Data in the request is missing or invalid",
                Instant.now().toString(),
                extractRequestMessageId(ex),
                UUID.randomUUID().toString(),
                details
        );

        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBusinessValidationException(ValidationException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                "RC2000",
                "Business Validation Failure: " + ex.getMessage(),
                Instant.now().toString(),
                "UNKNOWN",
                UUID.randomUUID().toString(),
                List.of()
        );

        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }


    @ExceptionHandler(R2PTransactionNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleTransactionNotFoundException(R2PTransactionNotFoundException ex) {
        log.error("Transaction not found: {}", ex.getMessage());
        return Mono.just(ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(createErrorResponse("RC4000", ex.getMessage())));
    }

    @ExceptionHandler(R2PTransactionValidationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(R2PTransactionValidationException ex) {
        log.error("Validation error: {}", ex.getMessage());
        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse("RC2000", ex.getMessage())));
    }

    @ExceptionHandler(R2PTransactionProcessingException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleProcessingException(R2PTransactionProcessingException ex) {
        log.error("Processing error: {}", ex.getMessage(), ex);
        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("RC5000", "Failed to process transaction")));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("RC5000", "An unexpected error occurred")));
    }

    private ErrorResponse createErrorResponse(String code, String message) {
        return new ErrorResponse(
                code,
                message,
                LocalDateTime.now().toString(),
                "ERR" + System.currentTimeMillis(),
                "ERR" + System.currentTimeMillis(),
                null
        );
    }

    private ErrorDetail createErrorDetail(FieldError fieldError) {
        return new ErrorDetail(
                fieldError.getField(),
                fieldError.getDefaultMessage()
        );
    }

    private String extractRequestMessageId(WebExchangeBindException ex) {
        // Try to extract requestMessageId from the request
        return "UNKNOWN";
    }
}