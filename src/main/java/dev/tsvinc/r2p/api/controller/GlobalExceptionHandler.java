package dev.tsvinc.r2p.api.controller;

import dev.tsvinc.r2p.api.dto.request.ErrorDetail;
import dev.tsvinc.r2p.api.dto.response.ExtendedErrorResponse;
import dev.tsvinc.r2p.api.dto.response.ErrorResponse;
import dev.tsvinc.r2p.domain.enums.R2PErrorCode;
import dev.tsvinc.r2p.exception.R2PBusinessException;
import dev.tsvinc.r2p.exception.R2PTransactionNotFoundException;
import dev.tsvinc.r2p.exception.R2PTransactionProcessingException;
import dev.tsvinc.r2p.exception.R2PTransactionValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
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
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
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

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ExtendedErrorResponse>> handleValidationException(WebExchangeBindException ex) {
        List<ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::createDetailedErrorDetail)
                .toList();

        ExtendedErrorResponse errorResponse = createErrorResponse(
                R2PErrorCode.RC1000,
                extractRequestMessageId(ex),
                details
        );

        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ResponseEntity<ExtendedErrorResponse>> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorDetail> details = ex.getConstraintViolations().stream()
                .map(this::createErrorDetailFromConstraint)
                .collect(Collectors.toList());

        ExtendedErrorResponse errorResponse = createErrorResponse(
                R2PErrorCode.RC1002,
                "UNKNOWN",
                details
        );

        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    @ExceptionHandler(R2PBusinessException.class)
    public Mono<ResponseEntity<ExtendedErrorResponse>> handleBusinessException(R2PBusinessException ex) {
        R2PErrorCode errorCode = determineBusinessErrorCode(ex.getMessage());
        ExtendedErrorResponse errorResponse = createErrorResponse(errorCode, "UNKNOWN", List.of());

        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    @ExceptionHandler(R2PTransactionNotFoundException.class)
    public Mono<ResponseEntity<ExtendedErrorResponse>> handleNotFound(R2PTransactionNotFoundException ex) {
        ExtendedErrorResponse errorResponse = createErrorResponse(
                R2PErrorCode.RC4001,
                "UNKNOWN",
                List.of()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ExtendedErrorResponse>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        ExtendedErrorResponse errorResponse = createErrorResponse(
                R2PErrorCode.RC5000,
                "UNKNOWN",
                List.of()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }

    private ExtendedErrorResponse createErrorResponse(R2PErrorCode errorCode, String requestMessageId, List<ErrorDetail> details) {
        return new ExtendedErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                Instant.now().toString(),
                requestMessageId,
                UUID.randomUUID().toString(),
                details
        );
    }

    private ErrorDetail createDetailedErrorDetail(FieldError fieldError) {
        String reason = determineValidationReason(fieldError);
        return new ErrorDetail(fieldError.getField(), reason);
    }

    private ErrorDetail createErrorDetailFromConstraint(ConstraintViolation<?> violation) {
        return new ErrorDetail(
                violation.getPropertyPath().toString(),
                violation.getMessage()
        );
    }

    private String determineValidationReason(FieldError fieldError) {
        String code = fieldError.getCode();
        return switch (code != null ? code : "") {
            case "NotNull", "NotBlank", "NotEmpty" -> "Field is required";
            case "Size" -> "Field length is invalid";
            case "Pattern" -> "Field format is invalid";
            case "DecimalMin" -> "Value is below minimum";
            case "Email" -> "Invalid email format";
            default -> fieldError.getDefaultMessage();
        };
    }

    private R2PErrorCode determineBusinessErrorCode(String message) {
        if (message.contains("amount exceeds")) return R2PErrorCode.RC2001;
        if (message.contains("currency")) return R2PErrorCode.RC2002;
        if (message.contains("MCC")) return R2PErrorCode.RC2003;
        if (message.contains("name")) return R2PErrorCode.RC2004;
        if (message.contains("due date")) return R2PErrorCode.RC2005;
        if (message.contains("refund")) return R2PErrorCode.RC2006;
        return R2PErrorCode.RC2000;
    }

    private String extractRequestMessageId(WebExchangeBindException ex) {
        // Extract from request body if available
        return "UNKNOWN";
    }
}