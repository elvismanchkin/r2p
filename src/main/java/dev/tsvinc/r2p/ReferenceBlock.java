package dev.tsvinc.r2p;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReferenceBlock(
        @NotBlank @Pattern(regexp = "CONTRACTID|INVOICEID|ORDERID|TRACKINGID|PAYMENTID|OTHER") String referenceType,
        @NotBlank @Size(max = 35) String referenceValue,
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String referenceDate
) {
}
