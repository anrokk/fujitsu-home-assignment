package ee.calculator.delivery_fee.exception;

import ee.calculator.delivery_fee.domain.City;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.RegionalBaseFee;

import java.time.Instant;

public class RegionalBaseFeeNotFoundException extends RuntimeException {
    public RegionalBaseFeeNotFoundException(
            City city, VehicleType vehicleType, Instant time) {
        super("No regional base fee found for %s, %s at %s"
                .formatted(city, vehicleType, time));
    }
}
