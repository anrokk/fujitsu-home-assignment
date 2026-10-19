package ee.calculator.delivery_fee.service;


import ee.calculator.delivery_fee.domain.City;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.exception.RegionalBaseFeeNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:base_fee_service_test"
)
public class RegionalBaseFeeServiceTest {
    private static final Instant time = Instant.parse("2026-10-01T00:00:00Z");

    @Autowired
    private RegionalBaseFeeService service;

    @ParameterizedTest
    @CsvSource({
            "TALLINN, CAR,     4.00",
            "TALLINN, SCOOTER, 3.50",
            "TALLINN, BIKE,    3.00",
            "TARTU,   CAR,     3.50",
            "TARTU,   SCOOTER, 3.00",
            "TARTU,   BIKE,    2.50",
            "PARNU,   CAR,     3.00",
            "PARNU,   SCOOTER, 2.50",
            "PARNU,   BIKE,    2.00"
    })
    void returnsSeededBaseFee(
            City city,
            VehicleType vehicleType,
            String expectedAmount
    ) {
        BigDecimal amount = service.getAmount(city, vehicleType, time);

        assertThat(amount).isEqualByComparingTo(expectedAmount);
    }

    @Test
    void throwsWhenNoFeeAppliesAtRequestedTime() {
        Instant beforeInitialFees = Instant.EPOCH.minusSeconds(1);

        assertThatThrownBy(() -> service.getAmount(
                City.TARTU,
                VehicleType.BIKE,
                beforeInitialFees
        ))
                .isInstanceOf(RegionalBaseFeeNotFoundException.class)
                .hasMessageContaining("TARTU")
                .hasMessageContaining("BIKE");
    }


}
