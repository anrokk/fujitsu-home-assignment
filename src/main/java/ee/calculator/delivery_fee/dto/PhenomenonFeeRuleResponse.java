package ee.calculator.delivery_fee.dto;

import ee.calculator.delivery_fee.domain.PhenomenonCategory;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.PhenomenonFeeRule;

import java.math.BigDecimal;
import java.time.Instant;

/** Describes a stored phenomenon rule and its validity period. */
public record PhenomenonFeeRuleResponse(
        Long id,
        VehicleType vehicleType,
        PhenomenonCategory phenomenonCategory,
        BigDecimal amount,
        boolean forbidden,
        Instant validFrom,
        Instant validTo
) {
    /** Converts a stored rule into its API representation. */
    public static PhenomenonFeeRuleResponse from(PhenomenonFeeRule rule) {
        return new PhenomenonFeeRuleResponse(
                rule.getId(),
                rule.getVehicleType(),
                rule.getPhenomenonCategory(),
                rule.getAmount(),
                rule.isForbidden(),
                rule.getValidFrom(),
                rule.getValidTo()
        );
    }
}
