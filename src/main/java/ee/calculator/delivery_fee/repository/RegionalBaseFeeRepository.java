package ee.calculator.delivery_fee.repository;

import ee.calculator.delivery_fee.domain.City;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.RegionalBaseFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RegionalBaseFeeRepository
        extends JpaRepository<RegionalBaseFee, Long> {
    /**
     * Finds the city and vehicle's base fee valid at the requested time.
     * The start time is inclusive and the end time is exclusive.
     */
    @Query("""
            SELECT fee
            FROM RegionalBaseFee fee
            WHERE fee.city = :city
            AND fee.vehicleType = :vehicleType
            AND fee.validFrom <= :time
            AND (fee.validTo IS NULL OR fee.validTo > :time)
           """
    )
    Optional<RegionalBaseFee> findApplicableFee(
            @Param("city") City city,
            @Param("vehicleType") VehicleType vehicleType,
            @Param("time")Instant time
    );
}
