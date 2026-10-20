package ee.calculator.delivery_fee.service;


import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.TemperatureFeeRule;
import ee.calculator.delivery_fee.repository.TemperatureFeeRuleRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TemperatureFeeService {
    private final TemperatureFeeRuleRepository repository;


    @Transactional(readOnly = true)
    public BigDecimal getAmount(
            VehicleType vehicleType,
            double temperature,
            Instant time
    ) {
        return repository.findApplicableRule(
                vehicleType,
                temperature,
                time
        )
                .map(TemperatureFeeRule::getAmount)
                .orElse(BigDecimal.ZERO);
    }
}
