package ee.calculator.delivery_fee.dto;

import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.WindFeeRule;

import java.math.BigDecimal;
import java.time.Instant;

/** Describes a stored wind rule and its validity period. */
public record WindFeeRuleResponse(
        Long id,
        VehicleType vehicleType,
        Double minWindSpeed,
        Double maxWindSpeed,
        boolean minInclusive,
        boolean maxInclusive,
        BigDecimal amount,
        boolean forbidden,
        Instant validFrom,
        Instant validTo
) {
    /** Converts a stored rule into its API representation. */
    public static WindFeeRuleResponse from(WindFeeRule rule) {
        return new WindFeeRuleResponse(
                rule.getId(),
                rule.getVehicleType(),
                rule.getMinWindSpeed(),
                rule.getMaxWindSpeed(),
                rule.isMinInclusive(),
                rule.isMaxInclusive(),
                rule.getAmount(),
                rule.isForbidden(),
                rule.getValidFrom(),
                rule.getValidTo()
        );
    }
}
