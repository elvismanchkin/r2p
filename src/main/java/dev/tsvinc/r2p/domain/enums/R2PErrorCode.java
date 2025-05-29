package dev.tsvinc.r2p.domain.enums;

import lombok.Getter;

@Getter
public enum R2PErrorCode {
    // RC1xxx - Invalid Request
    RC1000("Data in the request is missing or invalid"),
    RC1001("Request message ID is invalid"),
    RC1002("Mandatory field is missing"),
    RC1003("Field format is incorrect"),
    RC1004("Field length exceeds maximum allowed"),

    // RC2xxx - Business Validation Failure
    RC2000("Business Validation Failure"),
    RC2001("Payment amount exceeds limits"),
    RC2002("Invalid currency for country"),
    RC2003("MCC required for B2C transactions"),
    RC2004("Invalid debtor/creditor name format"),
    RC2005("Due date cannot be in the past"),
    RC2006("Refund amount exceeds original transaction"),

    // RC3xxx - Authorization Failure
    RC3000("Authorization failure"),
    RC3001("Request rejected due to active block control"),
    RC3002("Request rejected due to velocity/frequency control"),
    RC3003("Insufficient permissions for operation"),

    // RC4xxx - Entity Unknown
    RC4000("Entity not found or unknown"),
    RC4001("Payment request not found"),
    RC4002("Payer not reachable"),
    RC4003("Invalid agent ID"),

    // RC5xxx - Server Errors
    RC5000("Internal server error"),
    RC5001("Database connection failure"),
    RC5002("External service unavailable");

    private final String message;

    R2PErrorCode(String message) {
        this.message = message;
    }

    public String getCode() {
        return name();
    }
}