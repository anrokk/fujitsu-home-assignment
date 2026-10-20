package ee.calculator.delivery_fee.repository;


import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.WindFeeRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface WindFeeRuleRepository extends JpaRepository<WindFeeRule, Long> {
    @Query("""
            SELECT rule
            FROM WindFeeRule rule
            WHERE rule.vehicleType = :vehicleType
              AND rule.validFrom <= :time
              AND (rule.validTo IS NULL OR rule.validTo > :time)
              AND (
                  rule.minWindSpeed IS NULL
                  OR :windSpeed > rule.minWindSpeed
                  OR (
                      rule.minInclusive = TRUE
                      AND :windSpeed = rule.minWindSpeed
                  )
              )
              AND (
                  rule.maxWindSpeed IS NULL
                  OR :windSpeed < rule.maxWindSpeed
                  OR (
                      rule.maxInclusive = TRUE
                      AND :windSpeed = rule.maxWindSpeed
                  )
              )
            """)
    Optional<WindFeeRule> findApplicableRule(
            @Param("vehicleType") VehicleType vehicleType,
            @Param("windSpeed") double windSpeed,
            @Param("time") Instant time
    );
}
