package ee.calculator.delivery_fee.dto;

import ee.calculator.delivery_fee.domain.City;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.RegionalBaseFee;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Describes a base-fee version and its validity period.
 */
public record RegionalBaseFeeResponse(
        Long id,
        City city,
        VehicleType vehicleType,
        BigDecimal amount,
        Instant validFrom,
        Instant validTo
) {

    /**
     * Converts a stored base fee into its API representation.
     */
    public static RegionalBaseFeeResponse from(RegionalBaseFee fee) {
        return new RegionalBaseFeeResponse(
                fee.getId(),
                fee.getCity(),
                fee.getVehicleType(),
                fee.getAmount(),
                fee.getValidFrom(),
                fee.getValidTo()
        );
    }
}