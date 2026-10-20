package ee.calculator.delivery_fee.service;

import ee.calculator.delivery_fee.domain.PhenomenonCategory;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.PhenomenonFeeRule;
import ee.calculator.delivery_fee.exception.VehicleForbiddenException;
import ee.calculator.delivery_fee.repository.PhenomenonFeeRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:phenomenon_fee_service_test")
@Transactional
class PhenomenonFeeServiceTest {

    private static final Instant TIME =
            Instant.parse("2026-10-01T00:00:00Z");

    @Autowired
    private PhenomenonFeeService service;

    @Autowired
    private PhenomenonFeeRuleRepository repository;

    @ParameterizedTest
    @CsvSource({
            "BIKE,    Light snow shower, 1.00",
            "SCOOTER, Moderate snowfall, 1.00",
            "BIKE,    Light sleet,       1.00",
            "SCOOTER, Moderate sleet,    1.00",
            "BIKE,    Light shower,      0.50",
            "SCOOTER, Heavy rain,        0.50",
            "BIKE,    Fog,               0.00",
            "SCOOTER, Clear,             0.00",
            "CAR,     Light snow shower, 0.00",
            "CAR,     Light sleet,       0.00",
            "CAR,     Light rain,        0.00",
            "CAR,     Glaze,             0.00",
            "CAR,     Hail,              0.00",
            "CAR,     Thunder,           0.00"
    })
    void returnsExpectedPhenomenonFee(
            VehicleType vehicleType,
            String phenomenon,
            String expectedAmount
    ) {
        BigDecimal amount = service.getAmount(
                vehicleType,
                phenomenon,
                TIME
        );

        assertThat(amount).isEqualByComparingTo(expectedAmount);
    }

    @ParameterizedTest
    @CsvSource({
            "BIKE,    Glaze",
            "SCOOTER, Glaze",
            "BIKE,    Hail",
            "SCOOTER, Hail",
            "BIKE,    Thunderstorm with rain",
            "SCOOTER, Thunder"
    })
    void forbidsVehiclesInDangerousConditions(
            VehicleType vehicleType,
            String phenomenon
    ) {
        assertThatThrownBy(() -> service.getAmount(
                vehicleType,
                phenomenon,
                TIME
        ))
                .isInstanceOf(VehicleForbiddenException.class)
                .hasMessage("Usage of selected vehicle type is forbidden");
    }

    @Test
    void preservesHistoricalFeeAfterPriceChange() {
        Instant changeTime = TIME.plusSeconds(3600);

        PhenomenonFeeRule oldRule = repository.findApplicableRule(
                VehicleType.BIKE,
                PhenomenonCategory.SNOW,
                TIME
        ).orElseThrow();

        oldRule.setValidTo(changeTime);
        repository.save(oldRule);

        PhenomenonFeeRule replacement = new PhenomenonFeeRule();
        replacement.setVehicleType(VehicleType.BIKE);
        replacement.setPhenomenonCategory(PhenomenonCategory.SNOW);
        replacement.setAmount(new BigDecimal("1.50"));
        replacement.setForbidden(false);
        replacement.setValidFrom(changeTime);
        repository.save(replacement);

        BigDecimal beforeChange = service.getAmount(
                VehicleType.BIKE,
                "Light snow shower",
                changeTime.minusSeconds(1)
        );

        BigDecimal atChange = service.getAmount(
                VehicleType.BIKE,
                "Light snow shower",
                changeTime
        );

        assertThat(beforeChange).isEqualByComparingTo("1.00");
        assertThat(atChange).isEqualByComparingTo("1.50");
    }
}
