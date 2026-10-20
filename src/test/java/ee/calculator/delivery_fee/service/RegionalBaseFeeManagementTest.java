package ee.calculator.delivery_fee.service;

import ee.calculator.delivery_fee.domain.City;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.RegionalBaseFee;
import ee.calculator.delivery_fee.exception.FeeRuleConflictException;
import ee.calculator.delivery_fee.exception.FeeRuleNotFoundException;
import ee.calculator.delivery_fee.exception.RegionalBaseFeeNotFoundException;
import ee.calculator.delivery_fee.repository.RegionalBaseFeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:base_fee_management_test")
@Transactional
class RegionalBaseFeeManagementTest {

    private static final Instant TIME =
            Instant.parse("2026-10-20T17:00:00Z");

    @Autowired
    private RegionalBaseFeeService service;

    @Autowired
    private RegionalBaseFeeRepository repository;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(TIME);
    }

    @Test
    void rejectsCreatingDuplicateActiveFee() {
        long countBefore = repository.count();

        assertThatThrownBy(() -> service.create(
                City.TARTU,
                VehicleType.BIKE,
                new BigDecimal("3.00")))
                .isInstanceOf(FeeRuleConflictException.class);

        assertThat(repository.count()).isEqualTo(countBefore);
    }

    @Test
    void createsFeeAfterPreviousFeeWasRetired() {
        RegionalBaseFee original = currentFee();
        long countBefore = repository.count();

        service.delete(original.getId());

        Instant creationTime = TIME.plusSeconds(60);
        when(clock.instant()).thenReturn(creationTime);

        RegionalBaseFee created = service.create(
                City.TARTU,
                VehicleType.BIKE,
                new BigDecimal("3.00"));

        repository.flush();

        assertThat(created.getId()).isNotEqualTo(original.getId());
        assertThat(created.getValidFrom()).isEqualTo(creationTime);
        assertThat(created.getValidTo()).isNull();
        assertThat(repository.count()).isEqualTo(countBefore + 1);

        assertThat(service.getAmount(
                City.TARTU, VehicleType.BIKE, creationTime))
                .isEqualByComparingTo("3.00");

        assertThat(service.getAmount(
                City.TARTU, VehicleType.BIKE, TIME.minusSeconds(1)))
                .isEqualByComparingTo("2.50");

        assertThatThrownBy(() -> service.getAmount(
                City.TARTU, VehicleType.BIKE, TIME))
                .isInstanceOf(RegionalBaseFeeNotFoundException.class);
    }

    @Test
    void updatePreservesOldPriceAndStartsNewPriceAtChangeTime() {
        RegionalBaseFee original = currentFee();
        long countBefore = repository.count();

        RegionalBaseFee replacement = service.update(
                original.getId(),
                new BigDecimal("3.25"));

        repository.flush();

        assertThat(replacement.getId()).isNotEqualTo(original.getId());
        assertThat(replacement.getCity()).isEqualTo(City.TARTU);
        assertThat(replacement.getVehicleType()).isEqualTo(VehicleType.BIKE);
        assertThat(replacement.getValidFrom()).isEqualTo(TIME);
        assertThat(replacement.getValidTo()).isNull();
        assertThat(repository.count()).isEqualTo(countBefore + 1);

        RegionalBaseFee historical = service.findById(original.getId());

        assertThat(historical.getValidTo()).isEqualTo(TIME);
        assertThat(historical.getAmount()).isEqualByComparingTo("2.50");

        assertThat(service.getAmount(
                City.TARTU, VehicleType.BIKE, TIME.minusSeconds(1)))
                .isEqualByComparingTo("2.50");

        assertThat(service.getAmount(
                City.TARTU, VehicleType.BIKE, TIME))
                .isEqualByComparingTo("3.25");

        assertThat(service.getAmount(
                City.TARTU, VehicleType.BIKE, TIME.plusSeconds(1)))
                .isEqualByComparingTo("3.25");
    }

    @Test
    void deleteRetiresFeeWithoutRemovingHistory() {
        RegionalBaseFee original = currentFee();
        long countBefore = repository.count();

        service.delete(original.getId());

        repository.flush();

        assertThat(repository.count()).isEqualTo(countBefore);
        assertThat(service.findById(original.getId()).getValidTo())
                .isEqualTo(TIME);

        assertThat(service.getAmount(
                City.TARTU, VehicleType.BIKE, TIME.minusSeconds(1)))
                .isEqualByComparingTo("2.50");

        assertThatThrownBy(() -> service.getAmount(
                City.TARTU, VehicleType.BIKE, TIME))
                .isInstanceOf(RegionalBaseFeeNotFoundException.class);
    }

    @Test
    void rejectsUpdatingHistoricalVersion() {
        Long originalId = currentFee().getId();

        service.update(originalId, new BigDecimal("3.00"));

        when(clock.instant()).thenReturn(TIME.plusSeconds(60));

        assertThatThrownBy(() -> service.update(
                originalId, new BigDecimal("4.00")))
                .isInstanceOf(FeeRuleConflictException.class)
                .hasMessage("Historical fee versions cannot be changed");
    }

    @Test
    void rejectsDeletingHistoricalVersion() {
        Long originalId = currentFee().getId();

        service.delete(originalId);

        when(clock.instant()).thenReturn(TIME.plusSeconds(60));

        assertThatThrownBy(() -> service.delete(originalId))
                .isInstanceOf(FeeRuleConflictException.class)
                .hasMessage("Historical fee versions cannot be changed");
    }

    @Test
    void rejectsChangeAtFeeStartTime() {
        RegionalBaseFee original = currentFee();

        when(clock.instant()).thenReturn(original.getValidFrom());

        assertThatThrownBy(() -> service.update(
                original.getId(), new BigDecimal("3.00")))
                .isInstanceOf(FeeRuleConflictException.class)
                .hasMessage(
                        "The change time must be after the fee's start time");
    }

    @Test
    void rejectsUpdatingUnknownFee() {
        assertThatThrownBy(() -> service.update(
                -1L, new BigDecimal("3.00")))
                .isInstanceOf(FeeRuleNotFoundException.class);
    }

    private RegionalBaseFee currentFee() {
        return repository.findApplicableFee(
                        City.TARTU, VehicleType.BIKE, TIME)
                .orElseThrow();
    }
}