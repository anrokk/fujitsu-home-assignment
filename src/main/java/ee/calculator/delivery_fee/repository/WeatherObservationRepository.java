package ee.calculator.delivery_fee.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ee.calculator.delivery_fee.domain.City;
import ee.calculator.delivery_fee.entity.WeatherObservation;

public interface WeatherObservationRepository extends JpaRepository<WeatherObservation, Long> {
    /**
     * Finds the city's latest observation at or before the requested time.
     */
    Optional<WeatherObservation> findFirstByCityAndObservedAtLessThanEqualOrderByObservedAtDesc(City city,
            Instant time);

}
