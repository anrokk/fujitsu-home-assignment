package ee.calculator.delivery_fee.service;


import ee.calculator.delivery_fee.domain.PhenomenonCategory;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.exception.VehicleForbiddenException;
import ee.calculator.delivery_fee.repository.PhenomenonFeeRuleRepository;
import ee.calculator.delivery_fee.weather.PhenomenonClassifier;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PhenomenonFeeService {
    private final PhenomenonFeeRuleRepository repository;
    private final PhenomenonClassifier classifier;

    /**
     * Classifies the phenomenon and returns its surcharge at the requested time,
     * or zero when no rule applies.
     *
     * @throws VehicleForbiddenException if the matching rule prohibits the vehicle
     */
    @Transactional(readOnly = true)
    public BigDecimal getAmount(
            VehicleType vehicleType,
            String phenomenon,
            Instant time
    ) {
        PhenomenonCategory category = classifier.classify(phenomenon);

        return repository.findApplicableRule(vehicleType, category, time)
                .map(rule -> {
                    if (rule.isForbidden()) {
                        throw new VehicleForbiddenException();
                    }

                    return rule.getAmount();
                })
                .orElse(BigDecimal.ZERO);
    }
}
