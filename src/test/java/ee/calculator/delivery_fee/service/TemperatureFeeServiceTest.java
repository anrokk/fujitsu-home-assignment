package ee.calculator.delivery_fee.service;


import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.TemperatureFeeRule;
import ee.calculator.delivery_fee.repository.TemperatureFeeRuleRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:temperature_fee_service_test")
@Transactional
public class TemperatureFeeServiceTest {
    private static final Instant TIME = Instant.parse("2026-10-01T00:00:00Z");

    @Autowired
    private TemperatureFeeService service;

    @Autowired
    private TemperatureFeeRuleRepository repository;

    @ParameterizedTest
    @CsvSource({
            "BIKE,    -10.1, 1.00",
            "BIKE,    -10.0, 0.50",
            "BIKE,     -9.9, 0.50",
            "BIKE,      0.0, 0.50",
            "BIKE,      0.1, 0.00",
            "SCOOTER, -10.1, 1.00",
            "SCOOTER, -10.0, 0.50",
            "SCOOTER,  -9.9, 0.50",
            "SCOOTER,   0.0, 0.50",
            "SCOOTER,   0.1, 0.00",
            "CAR,     -10.1, 0.00",
            "CAR,     -10.0, 0.00",
            "CAR,       0.0, 0.00",
            "CAR,       0.1, 0.00"
    })
    void returnsExpectedTemperatureFee(
            VehicleType vehicleType,
            double temperature,
            String expectedAmount
    ) {
        BigDecimal actualAmount = service.getAmount(vehicleType, temperature, TIME);
        assertThat(actualAmount).isEqualByComparingTo(expectedAmount);
    }

    @Test
    void preservesHistoricalFeeAfterPriceChange() {
        Instant changeTime = TIME.plusSeconds(3600); // add 1 hour

        TemperatureFeeRule oldRule = repository.findApplicableRule(
                VehicleType.BIKE,
                -20.0,
                TIME
        ).orElseThrow();

        oldRule.setValidTo(changeTime);
        repository.save(oldRule);

        TemperatureFeeRule replacement = new TemperatureFeeRule();
        replacement.setVehicleType(VehicleType.BIKE);
        replacement.setMaxTemperature(-10.0);
        replacement.setMaxInclusive(false);
        replacement.setAmount(new BigDecimal("1.50"));
        replacement.setValidFrom(changeTime);
        repository.save(replacement);

        BigDecimal beforeChange = service.getAmount(
                VehicleType.BIKE,
                -20.0,
                changeTime.minusSeconds(1)
        );

        BigDecimal atChange = service.getAmount(
                VehicleType.BIKE,
                -20.0,
                changeTime
        );

        assertThat(beforeChange).isEqualByComparingTo("1.00");
        assertThat(atChange).isEqualByComparingTo("1.50");

    }
}
