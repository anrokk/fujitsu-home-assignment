package ee.calculator.delivery_fee.service;


import ee.calculator.delivery_fee.domain.City;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.WeatherObservation;
import ee.calculator.delivery_fee.exception.WeatherDataUnavailableException;
import ee.calculator.delivery_fee.repository.WeatherObservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DeliveryFeeService {
    private final WeatherObservationRepository weatherRepository;
    private final RegionalBaseFeeService regionalBaseFeeService;
    private final TemperatureFeeService temperatureFeeService;
    private final WindFeeService windFeeService;
    private final PhenomenonFeeService phenomenonFeeService;

    /**
     * Calculates the total fee using rules valid at the supplied time
     * and the latest weather observation at or before that time
     * @param city The city for which the fee is calculated
     * @param vehicleType The vehicle type for which the fee is calculated
     * @param time The time at which the fee is calculated
     * @return The total fee for the delivery
     */
    @Transactional(readOnly = true)
    public BigDecimal calculate(
            City city,
            VehicleType vehicleType,
            Instant time
    ) {
        BigDecimal baseFee = regionalBaseFeeService.getAmount(city, vehicleType, time);

        if (vehicleType == VehicleType.CAR) {
            return baseFee;
        }

        WeatherObservation weather = weatherRepository
                .findFirstByCityAndObservedAtLessThanEqualOrderByObservedAtDesc(city, time)
                .orElseThrow(() -> new WeatherDataUnavailableException(
                        "No weather observation found for %s at or before %s"
                                .formatted(city, time)
                ));

        BigDecimal phenomenonFee = phenomenonFeeService.getAmount(
                vehicleType,
                weather.getPhenomenon(),
                time
        );

        BigDecimal windFee = BigDecimal.ZERO;

        if (vehicleType == VehicleType.BIKE) {
            if (weather.getWindSpeed() == null) {
                throw new WeatherDataUnavailableException(
                        "Wind speed is missing for " + city
                );
            }

            windFee = windFeeService.getAmount(
                    vehicleType,
                    weather.getWindSpeed(),
                    time
            );
        }

        if (weather.getAirTemperature() == null) {
            throw new WeatherDataUnavailableException(
                    "Air temperature is missing for " + city
            );
        }

        BigDecimal temperatureFee = temperatureFeeService.getAmount(
                vehicleType,
                weather.getAirTemperature(),
                time
        );

        return baseFee
                .add(phenomenonFee)
                .add(windFee)
                .add(temperatureFee);
    }
}
