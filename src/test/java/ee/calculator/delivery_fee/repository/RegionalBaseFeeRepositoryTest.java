package ee.calculator.delivery_fee.repository;


import ee.calculator.delivery_fee.domain.City;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.RegionalBaseFee;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:base_fee_repository_test")
@Transactional
public class RegionalBaseFeeRepositoryTest {
    private static final Instant START =
            Instant.parse("2026-06-01T00:00:00Z");

    private static final Instant CHANGE =
            Instant.parse("2026-09-01T00:00:00Z");

    @Autowired
    private RegionalBaseFeeRepository repository;

    @BeforeEach
    void setUpTartuBikePriceChange() {
        repository.deleteAll();

        RegionalBaseFee oldFee = new RegionalBaseFee();
        oldFee.setCity(City.TARTU);
        oldFee.setVehicleType(VehicleType.BIKE);
        oldFee.setAmount(new BigDecimal("2.50"));
        oldFee.setValidFrom(START);
        oldFee.setValidTo(CHANGE);
        repository.save(oldFee);

        RegionalBaseFee newFee = new RegionalBaseFee();
        newFee.setCity(City.TARTU);
        newFee.setVehicleType(VehicleType.BIKE);
        newFee.setAmount(new BigDecimal("3.00"));
        newFee.setValidFrom(CHANGE);
        newFee.setValidTo(null);
        repository.save(newFee);
    }

    @Test
    void returnsOldPriceBeforeChange() {
        Instant requestedTime = CHANGE.minusSeconds(1);

        RegionalBaseFee result = repository.findApplicableFee(
                City.TARTU,
                VehicleType.BIKE,
                requestedTime
        ).orElseThrow();

        assertThat(result.getAmount()).isEqualByComparingTo("2.50");
    }

    @Test
    void returnsNewPriceExactlyAtChange() {
        RegionalBaseFee result = repository.findApplicableFee(
                City.TARTU,
                VehicleType.BIKE,
                CHANGE
        ).orElseThrow();

        assertThat(result.getAmount()).isEqualByComparingTo("3.00");
    }

    @Test
    void returnsEmptyBeforeFirstFeeStarts() {
        assertThat(repository.findApplicableFee(
                City.TARTU,
                VehicleType.BIKE,
                START.minusSeconds(1)
        )).isEmpty();
    }

    @Test
    void doesNotMatchAnotherCityOrVehicle() {
        assertThat(repository.findApplicableFee(
                City.TALLINN,
                VehicleType.BIKE,
                CHANGE
        )).isEmpty();

        assertThat(repository.findApplicableFee(
                City.TARTU,
                VehicleType.CAR,
                CHANGE
        )).isEmpty();
    }
}
