package dev.tsvinc.r2p.service.validation;

import dev.tsvinc.r2p.api.dto.request.AmendR2pRequest;
import dev.tsvinc.r2p.api.dto.request.CancelR2pRequest;
import dev.tsvinc.r2p.api.dto.request.ConfirmR2pRequest;
import dev.tsvinc.r2p.api.dto.request.Creditor;
import dev.tsvinc.r2p.api.dto.request.InitiateR2pRequest;
import dev.tsvinc.r2p.api.dto.request.PaymentRequestDetail;
import dev.tsvinc.r2p.api.dto.request.RefundR2pRequest;
import dev.tsvinc.r2p.domain.enums.UseCase;
import dev.tsvinc.r2p.exception.R2PBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class R2PBusinessValidationService {

    private static final Map<String, String> COUNTRY_CURRENCIES = Map.of(
            "UA", "UAH", "US", "USD", "GB", "GBP", "DE", "EUR", "FR", "EUR"
    );

    private static final Map<String, BigDecimal> COUNTRY_LIMITS = Map.of(
            "UA", BigDecimal.valueOf(50000), "US", BigDecimal.valueOf(10000),
            "GB", BigDecimal.valueOf(8500), "DE", BigDecimal.valueOf(10000)
    );

    private static final Set<String> SUPPORTED_MCCS = Set.of(
            "1234", "5411", "5812", "7995", "6011", "4899"
    );

    private static final Pattern UA_PHONE = Pattern.compile("^\\+380\\d{9}$");
    private static final Pattern US_PHONE = Pattern.compile("^\\+1\\d{10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    public void validateInitiateRequest(InitiateR2pRequest request) {
        validateCreditor(request.creditor(), request.useCase());
        validateDueDate(request.dueDate());

        for (PaymentRequestDetail paymentRequest : request.paymentRequests()) {
            validatePaymentRequest(paymentRequest, request.useCase());
        }
    }

    public void validateConfirmRequest(ConfirmR2pRequest request) {
        if (request.requestMessageId() == null || request.requestMessageId().isBlank()) {
            throw new R2PBusinessException("Request message ID is required");
        }
        if (request.transactionStatus() == null) {
            throw new R2PBusinessException("Transaction status is required");
        }
    }

    public void validateCancelRequest(CancelR2pRequest request) {
        if (request.requestMessageId() == null || request.requestMessageId().isBlank()) {
            throw new R2PBusinessException("Request message ID is required");
        }
        if (request.cancellationReason() == null || request.cancellationReason().isBlank()) {
            throw new R2PBusinessException("Cancellation reason is required");
        }
    }

    public void validateAmendRequest(AmendR2pRequest request) {
        if (request.requestMessageId() == null || request.requestMessageId().isBlank()) {
            throw new R2PBusinessException("Request message ID is required");
        }
        if (request.dueDate() != null) {
            validateDueDate(request.dueDate());
        }
    }

    public void validateRefundRequest(RefundR2pRequest request) {
        if (request.requestMessageId() == null || request.requestMessageId().isBlank()) {
            throw new R2PBusinessException("Request message ID is required");
        }
        if (request.paymentRequests() == null || request.paymentRequests().isEmpty()) {
            throw new R2PBusinessException("Payment request details are required");
        }
    }

    public void validateCreditor(Creditor creditor, UseCase useCase) {
        // Country validation
        validateCountryCode(creditor.creditorCountry());
        validateCountryCode(creditor.creditorAgentCountry());

        // Use case specific validation
        if (useCase == UseCase.B2C) {
            validateB2CCreditor(creditor);
        } else {
            validateP2PCreditor(creditor);
        }

        // Alias validation
        if (creditor.creditorAlias() != null) {
            validateAlias(creditor.creditorAlias(), creditor.creditorAliasType(), creditor.creditorCountry());
        }
    }

    public void validatePaymentRequest(PaymentRequestDetail request, UseCase useCase) {
        // Country validation
        validateCountryCode(request.debtorCountry());
        validateCountryCode(request.debtorAgentCountry());

        // Amount limits by country
        validateAmountLimits(request.requestedAmount(), request.debtorCountry());

        // Currency validation
        validateCurrency(request.requestedAmountCurrency(), request.debtorCountry());

        // Alias validation
        validateAlias(request.debtorAlias(), request.debtorAliasType(), request.debtorCountry());

        // Name validation by country
        validateNameFormat(request.debtorFirstName(), request.debtorLastName(), request.debtorCountry());
    }

    private void validateB2CCreditor(Creditor creditor) {
        if (creditor.creditorBusinessName() == null || creditor.creditorBusinessName().isBlank()) {
            throw new R2PBusinessException("Business name is required for B2C transactions");
        }

        if (creditor.creditorMcc() == null || !SUPPORTED_MCCS.contains(creditor.creditorMcc())) {
            throw new R2PBusinessException("Valid MCC is required for B2C transactions");
        }

        // Tax ID validation for certain countries
        if (("UA".equals(creditor.creditorCountry()) || "DE".equals(creditor.creditorCountry()))
                && creditor.creditorTaxId() == null) {
            throw new R2PBusinessException("Tax ID is required for B2C in " + creditor.creditorCountry());
        }
    }

    private void validateP2PCreditor(Creditor creditor) {
        if (creditor.creditorFirstName() == null || creditor.creditorLastName() == null) {
            throw new R2PBusinessException("First and last name are required for P2P transactions");
        }

        validateNameFormat(creditor.creditorFirstName(), creditor.creditorLastName(), creditor.creditorCountry());
    }

    private void validateCountryCode(String countryCode) {
        Set<String> supportedCountries = Set.of("UA", "US", "GB", "DE", "FR", "PL");
        if (!supportedCountries.contains(countryCode)) {
            throw new R2PBusinessException("Unsupported country: " + countryCode);
        }
    }

    private void validateAmountLimits(BigDecimal amount, String country) {
        BigDecimal limit = COUNTRY_LIMITS.get(country);
        if (limit != null && amount.compareTo(limit) > 0) {
            throw new R2PBusinessException("Amount exceeds country limit for " + country);
        }

        if (amount.compareTo(BigDecimal.valueOf(0.01)) < 0) {
            throw new R2PBusinessException("Amount must be at least 0.01");
        }
    }

    private void validateCurrency(String currency, String country) {
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            throw new R2PBusinessException("Invalid currency code: " + currency);
        }

        String expectedCurrency = COUNTRY_CURRENCIES.get(country);
        if (expectedCurrency != null && !expectedCurrency.equals(currency)) {
            throw new R2PBusinessException("Currency " + currency + " not supported for country " + country);
        }
    }

    private void validateAlias(String alias, Object aliasType, String country) {
        if (aliasType == null) return;

        switch (aliasType.toString()) {
            case "MOBL" -> validatePhoneNumber(alias, country);
            case "EMAIL" -> validateEmail(alias);
        }
    }

    private void validatePhoneNumber(String phone, String country) {
        boolean valid = switch (country) {
            case "UA" -> UA_PHONE.matcher(phone).matches();
            case "US" -> US_PHONE.matcher(phone).matches();
            default -> phone.startsWith("+") && phone.length() >= 10 && phone.length() <= 15;
        };

        if (!valid) {
            throw new R2PBusinessException("Invalid phone number format for country " + country);
        }
    }

    private void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new R2PBusinessException("Invalid email format");
        }
    }

    private void validateNameFormat(String firstName, String lastName, String country) {
        // Ukraine, Belarus, Kazakhstan require first letter + dot format for last name
        if (Set.of("UA", "BY", "KZ").contains(country)) {
            if (!Pattern.matches("^[A-Z]\\.?$", lastName)) {
                throw new R2PBusinessException("Last name must be first letter + dot for country " + country);
            }
        }

        // General name validation
        if (firstName != null && firstName.length() > 140) {
            throw new R2PBusinessException("First name exceeds maximum length");
        }

        if (lastName != null && lastName.length() > 140) {
            throw new R2PBusinessException("Last name exceeds maximum length");
        }
    }

    private void validateDueDate(String dueDate) {
        if (dueDate != null) {
            LocalDate due = LocalDate.parse(dueDate);
            if (due.isBefore(LocalDate.now())) {
                throw new R2PBusinessException("Due date cannot be in the past");
            }

            if (due.isAfter(LocalDate.now().plusDays(90))) {
                throw new R2PBusinessException("Due date cannot be more than 90 days in the future");
            }
        }
    }
}