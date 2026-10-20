package ee.calculator.delivery_fee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Defines the replacement amount for an active base fee.
 */
public record UpdateRegionalBaseFeeRequest(
        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 8, fraction = 2)
        BigDecimal amount
) {
}