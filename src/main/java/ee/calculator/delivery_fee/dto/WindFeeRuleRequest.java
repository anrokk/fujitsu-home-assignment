package ee.calculator.delivery_fee.dto;

import ee.calculator.delivery_fee.domain.VehicleType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Defines a wind rule that takes effect immediately. */
public record WindFeeRuleRequest(
        @NotNull VehicleType vehicleType,
        Double minWindSpeed,
        Double maxWindSpeed,
        @NotNull Boolean minInclusive,
        @NotNull Boolean maxInclusive,
        @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal amount,
        @NotNull Boolean forbidden
) {
    /** Checks that bounds are finite, ordered, and not both absent. */
    @AssertTrue(message = "Provide at least one finite bound, with minimum less than maximum")
    public boolean isRangeValid() {
        return (minWindSpeed != null || maxWindSpeed != null)
                && (minWindSpeed == null || Double.isFinite(minWindSpeed))
                && (maxWindSpeed == null || Double.isFinite(maxWindSpeed))
                && (minWindSpeed == null || maxWindSpeed == null || minWindSpeed < maxWindSpeed)
                && (minWindSpeed == null || minWindSpeed >= 0)
                && (maxWindSpeed == null || maxWindSpeed >= 0);
    }

    /** Checks that this rule applies to a supported vehicle type. */
    @AssertTrue(message = "Unsupported vehicle type for wind rules")
    public boolean isVehicleSupported() {
        return vehicleType == null || vehicleType == VehicleType.BIKE;
    }

    /** Checks that a forbidden rule has no amount and a surcharge rule has one. */
    @AssertTrue(message = "Forbidden rules require a null amount; other rules require an amount")
    public boolean isOutcomeValid() {
        return Boolean.TRUE.equals(forbidden) ? amount == null : amount != null;
    }
}
