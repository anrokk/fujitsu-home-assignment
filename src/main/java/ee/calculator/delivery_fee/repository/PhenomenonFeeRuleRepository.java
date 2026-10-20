package ee.calculator.delivery_fee.repository;

import ee.calculator.delivery_fee.domain.PhenomenonCategory;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.PhenomenonFeeRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PhenomenonFeeRuleRepository extends JpaRepository<PhenomenonFeeRule, Long> {
    @Query("""
            SELECT rule
            FROM PhenomenonFeeRule rule
            WHERE rule.vehicleType = :vehicleType
              AND rule.phenomenonCategory = :category
              AND rule.validFrom <= :time
              AND (rule.validTo IS NULL OR rule.validTo > :time)
            """)
    Optional<PhenomenonFeeRule> findApplicableRule(
            @Param("vehicleType") VehicleType vehicleType,
            @Param("category") PhenomenonCategory category,
            @Param("time") Instant time
    );
}
