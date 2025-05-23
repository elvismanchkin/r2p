package dev.tsvinc.r2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RequestOptions(
        Boolean allowMultiplePayments,
        Boolean closeWithFirstPayment,
        @Size(max = 10) List<@Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z") String> reminderSchedule
) {
    public RequestOptions {
        // Business validation
        if (Boolean.TRUE.equals(allowMultiplePayments) && Boolean.TRUE.equals(closeWithFirstPayment)) {
            throw new IllegalArgumentException("Cannot have both allowMultiplePayments and closeWithFirstPayment as true");
        }
    }
}
