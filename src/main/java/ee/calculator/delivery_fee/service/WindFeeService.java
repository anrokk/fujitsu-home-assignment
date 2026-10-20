package ee.calculator.delivery_fee.service;


import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.exception.VehicleForbiddenException;
import ee.calculator.delivery_fee.repository.WindFeeRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class WindFeeService {

    private final WindFeeRuleRepository repository;

    @Transactional(readOnly = true)
    public BigDecimal getAmount(
            VehicleType vehicleType,
            double windSpeed,
            Instant time
    ) {
        return repository.findApplicableRule(vehicleType, windSpeed, time)
                .map(rule -> {
                    if (rule.isForbidden()) {
                        throw new VehicleForbiddenException();
                    }
                    return rule.getAmount();
                })
                .orElse(BigDecimal.ZERO);
    }
}
