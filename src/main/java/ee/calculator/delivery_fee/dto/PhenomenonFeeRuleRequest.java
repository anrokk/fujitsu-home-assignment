package ee.calculator.delivery_fee.dto;

import ee.calculator.delivery_fee.domain.PhenomenonCategory;
import ee.calculator.delivery_fee.domain.VehicleType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Defines a phenomenon rule that takes effect immediately. */
public record PhenomenonFeeRuleRequest(
        @NotNull VehicleType vehicleType,
        @NotNull PhenomenonCategory phenomenonCategory,
        @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal amount,
        @NotNull Boolean forbidden
) {
    /** Checks that a forbidden rule has no amount and a surcharge rule has one. */
    @AssertTrue(message = "Forbidden rules require a null amount; other rules require an amount")
    public boolean isOutcomeValid() {
        return Boolean.TRUE.equals(forbidden) ? amount == null : amount != null;
    }

    /** Checks that this rule applies to a scooter or bike. */
    @AssertTrue(message = "Phenomenon rules apply only to scooters and bikes")
    public boolean isVehicleSupported() {
        return vehicleType == null || vehicleType == VehicleType.SCOOTER || vehicleType == VehicleType.BIKE;
    }
}
