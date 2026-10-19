package ee.calculator.delivery_fee.service;


import ee.calculator.delivery_fee.domain.City;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.exception.RegionalBaseFeeNotFoundException;
import ee.calculator.delivery_fee.repository.RegionalBaseFeeRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RegionalBaseFeeService {
    private final RegionalBaseFeeRepository repository;

    @Transactional(readOnly = true)
    public BigDecimal getAmount(
            City city,
            VehicleType vehicleType,
            Instant time
    ) {
        return repository.findApplicableFee(city, vehicleType, time)
                .orElseThrow(() -> new RegionalBaseFeeNotFoundException(
                        city,
                        vehicleType,
                        time
                ))
                .getAmount();
    }
}
