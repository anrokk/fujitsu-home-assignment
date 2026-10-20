package ee.calculator.delivery_fee.repository;

import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.TemperatureFeeRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface TemperatureFeeRuleRepository extends JpaRepository<TemperatureFeeRule, Long> {

    @Query("""
            SELECT rule
            FROM TemperatureFeeRule rule
            WHERE rule.vehicleType = :vehicleType
                AND rule.validFrom <= :time
                AND (rule.validTo IS NULL OR rule.validTo > :time)
                AND (
                           rule.minTemperature IS NULL
                           OR :temperature > rule.minTemperature
                           OR (
                               rule.minInclusive = TRUE
                               AND :temperature = rule.minTemperature
                           )
                    )
                AND (
                     rule.maxTemperature IS NULL
                     OR :temperature < rule.maxTemperature
                     OR (
                         rule.maxInclusive = TRUE
                         AND :temperature = rule.maxTemperature
                     )
                )
           """)
    Optional<TemperatureFeeRule> findApplicableRule(
            @Param("vehicleType") VehicleType vehicleType,
            @Param("temperature") double temperature,
            @Param("time") Instant time
    );
}
