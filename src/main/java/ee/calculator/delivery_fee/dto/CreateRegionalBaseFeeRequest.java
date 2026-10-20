package ee.calculator.delivery_fee.dto;

import ee.calculator.delivery_fee.domain.City;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.exception.VehicleForbiddenException;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;


/**
 * Defines a new base fee that takes effect immediately.
 */
public record CreateRegionalBaseFeeRequest(
        @NotNull City city,
        @NotNull VehicleType vehicleType,
        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 8, fraction = 2)
        BigDecimal amount
) {
}
