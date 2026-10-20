package ee.calculator.delivery_fee.dto;

import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.TemperatureFeeRule;

import java.math.BigDecimal;
import java.time.Instant;

/** Describes a stored temperature rule and its validity period. */
public record TemperatureFeeRuleResponse(
        Long id,
        VehicleType vehicleType,
        Double minTemperature,
        Double maxTemperature,
        boolean minInclusive,
        boolean maxInclusive,
        BigDecimal amount,
        Instant validFrom,
        Instant validTo
) {
    /** Converts a stored rule into its API representation. */
    public static TemperatureFeeRuleResponse from(TemperatureFeeRule rule) {
        return new TemperatureFeeRuleResponse(
                rule.getId(),
                rule.getVehicleType(),
                rule.getMinTemperature(),
                rule.getMaxTemperature(),
                rule.isMinInclusive(),
                rule.isMaxInclusive(),
                rule.getAmount(),
                rule.getValidFrom(),
                rule.getValidTo()
        );
    }
}
