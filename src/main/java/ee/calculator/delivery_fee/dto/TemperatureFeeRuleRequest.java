package ee.calculator.delivery_fee.dto;

import ee.calculator.delivery_fee.domain.VehicleType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Defines a temperature rule that takes effect immediately. */
public record TemperatureFeeRuleRequest(
        @NotNull VehicleType vehicleType,
        Double minTemperature,
        Double maxTemperature,
        @NotNull Boolean minInclusive,
        @NotNull Boolean maxInclusive,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal amount
) {
    /** Checks that bounds are finite, ordered, and not both absent. */
    @AssertTrue(message = "Provide at least one finite bound, with minimum less than maximum")
    public boolean isRangeValid() {
        return (minTemperature != null || maxTemperature != null)
                && (minTemperature == null || Double.isFinite(minTemperature))
                && (maxTemperature == null || Double.isFinite(maxTemperature))
                && (minTemperature == null || maxTemperature == null || minTemperature < maxTemperature);
    }

    /** Checks that this rule applies to a supported vehicle type. */
    @AssertTrue(message = "Unsupported vehicle type for temperature rules")
    public boolean isVehicleSupported() {
        return vehicleType == null || vehicleType == VehicleType.SCOOTER || vehicleType == VehicleType.BIKE;
    }
}
