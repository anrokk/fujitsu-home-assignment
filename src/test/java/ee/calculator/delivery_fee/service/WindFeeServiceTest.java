package ee.calculator.delivery_fee.service;


import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.WindFeeRule;
import ee.calculator.delivery_fee.exception.VehicleForbiddenException;
import ee.calculator.delivery_fee.repository.WindFeeRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:wind_fee_service_test")
@Transactional
public class WindFeeServiceTest {
    private static final Instant TIME = Instant.parse("2026-10-01T00:00:00Z");

    @Autowired
    private WindFeeService service;

    @Autowired
    private WindFeeRuleRepository repository;

    @ParameterizedTest
    @CsvSource({
            "BIKE,     0.0, 0.00",
            "BIKE,     9.9, 0.00",
            "BIKE,    10.0, 0.50",
            "BIKE,    19.9, 0.50",
            "BIKE,    20.0, 0.50",
            "SCOOTER, 10.0, 0.00",
            "SCOOTER, 25.0, 0.00",
            "CAR,     10.0, 0.00",
            "CAR,     25.0, 0.00"
    })
    void returnsExpectedWindFee(
            VehicleType vehicleType,
            double windSpeed,
            String expectedAmount
    ) {
        BigDecimal actualAmount = service.getAmount(vehicleType, windSpeed, TIME);

        assertThat(actualAmount).isEqualByComparingTo(expectedAmount);
    }

    @ParameterizedTest
    @ValueSource(doubles = {20.1, 30.0})
    void forbidsBikeAboveTwentyMetersPerSecond(double windSpeed) {
        assertThatThrownBy(() -> service.getAmount(
                VehicleType.BIKE,
                windSpeed,
                TIME
        ))
                .isInstanceOf(VehicleForbiddenException.class)
                .hasMessage("Usage of selected vehicle type is forbidden");
    }

    @Test
    void preservesHistoricalProhibitionAfterRuleIsRetired() {
        Instant changeTime = TIME.plusSeconds(3600);

        WindFeeRule prohibition = repository.findApplicableRule(
                VehicleType.BIKE,
                25.0,
                TIME
        ).orElseThrow();

        prohibition.setValidTo(changeTime);
        repository.save(prohibition);

        assertThatThrownBy(() -> service.getAmount(
                VehicleType.BIKE,
                25.0,
                changeTime.minusSeconds(1)
        )).isInstanceOf(VehicleForbiddenException.class);

        assertThat(service.getAmount(
                VehicleType.BIKE,
                25.0,
                changeTime
        )).isZero();
    }

}
