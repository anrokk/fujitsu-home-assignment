package ee.calculator.delivery_fee.service;

import ee.calculator.delivery_fee.domain.City;
import ee.calculator.delivery_fee.domain.PhenomenonCategory;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.dto.PhenomenonFeeRuleRequest;
import ee.calculator.delivery_fee.dto.TemperatureFeeRuleRequest;
import ee.calculator.delivery_fee.dto.WindFeeRuleRequest;
import ee.calculator.delivery_fee.exception.FeeRuleConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:fee_rule_concurrency_test;LOCK_TIMEOUT=10000")
class FeeRuleConcurrencyTest {

    private static final Instant TIME = Instant.parse("2026-10-21T10:00:00Z");
    private static final BigDecimal AMOUNT = new BigDecimal("1.25");

    @Autowired private RegionalBaseFeeService baseService;
    @Autowired private TemperatureFeeService temperatureService;
    @Autowired private WindFeeService windService;
    @Autowired private PhenomenonFeeService phenomenonService;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private Clock clock;

    enum RuleKind {
        BASE("regional_base_fee"), TEMPERATURE("temperature_fee_rule"),
        WIND("wind_fee_rule"), PHENOMENON("phenomenon_fee_rule");

        final String table;

        RuleKind(String table) {
            this.table = table;
        }
    }

    @BeforeEach
    void setUp() {
        for (RuleKind kind : RuleKind.values()) {
            jdbc.update("DELETE FROM " + kind.table);
        }
        when(clock.instant()).thenReturn(TIME);
    }

    @ParameterizedTest
    @EnumSource(RuleKind.class)
    void concurrentCreatesLeaveOneActiveRule(RuleKind kind) throws Exception {
        assertConcurrentConflict(() -> create(kind), () -> create(kind), false);
        assertVersionCounts(kind, 1);
    }

    @ParameterizedTest
    @EnumSource(RuleKind.class)
    void concurrentUpdatesLeaveOneReplacement(RuleKind kind) throws Exception {
        long id = create(kind);
        when(clock.instant()).thenReturn(TIME.plusSeconds(60));

        assertConcurrentConflict(() -> update(kind, id), () -> update(kind, id), false);
        assertVersionCounts(kind, 2);
    }

    @ParameterizedTest
    @EnumSource(RuleKind.class)
    void deleteCannotRetireAnAlreadyReplacedVersion(RuleKind kind) throws Exception {
        long id = create(kind);
        when(clock.instant()).thenReturn(TIME.plusSeconds(60));

        assertConcurrentConflict(() -> update(kind, id), () -> delete(kind, id), false);
        assertVersionCounts(kind, 2);
    }

    @Test
    void rollbackReleasesLockAndAllowsWaitingCreate() throws Exception {
        assertConcurrentConflict(() -> create(RuleKind.BASE), () -> create(RuleKind.BASE), true);
        assertVersionCounts(RuleKind.BASE, 1);
    }

    private void assertConcurrentConflict(Runnable firstChange, Runnable secondChange,
                                          boolean rollbackFirst) throws Exception {
        CountDownLatch firstChanged = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        firstChange.run();
                        firstChanged.countDown();
                        await(releaseFirst);
                        if (rollbackFirst) {
                            status.setRollbackOnly();
                        }
                    }));

            try {
                assertThat(firstChanged.await(5, TimeUnit.SECONDS)).isTrue();
                var second = executor.submit(() -> {
                    secondStarted.countDown();
                    secondChange.run();
                });

                try {
                    assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
                    // The service call must wait for the first transaction to finish.
                    assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS))
                            .isInstanceOf(TimeoutException.class);
                } finally {
                    releaseFirst.countDown();
                }

                first.get(5, TimeUnit.SECONDS);
                if (rollbackFirst) {
                    second.get(5, TimeUnit.SECONDS);
                } else {
                    assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS))
                            .isInstanceOf(ExecutionException.class)
                            .hasCauseInstanceOf(FeeRuleConflictException.class);
                }
            } finally {
                releaseFirst.countDown();
            }
        }
    }

    private void assertVersionCounts(RuleKind kind, int total) {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + kind.table, Integer.class))
                .isEqualTo(total);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + kind.table + " WHERE valid_to IS NULL", Integer.class))
                .isEqualTo(1);
    }

    private long create(RuleKind kind) {
        return switch (kind) {
            case BASE -> baseService.create(City.TARTU, VehicleType.BIKE, AMOUNT).getId();
            case TEMPERATURE -> temperatureService.create(temperatureRequest()).getId();
            case WIND -> windService.create(windRequest()).getId();
            case PHENOMENON -> phenomenonService.create(phenomenonRequest()).getId();
        };
    }

    private void update(RuleKind kind, long id) {
        switch (kind) {
            case BASE -> baseService.update(id, AMOUNT);
            case TEMPERATURE -> temperatureService.update(id, temperatureRequest());
            case WIND -> windService.update(id, windRequest());
            case PHENOMENON -> phenomenonService.update(id, phenomenonRequest());
        }
    }

    private void delete(RuleKind kind, long id) {
        switch (kind) {
            case BASE -> baseService.delete(id);
            case TEMPERATURE -> temperatureService.delete(id);
            case WIND -> windService.delete(id);
            case PHENOMENON -> phenomenonService.delete(id);
        }
    }

    private TemperatureFeeRuleRequest temperatureRequest() {
        return new TemperatureFeeRuleRequest(VehicleType.BIKE, -10.0, 0.0, true, true, AMOUNT);
    }

    private WindFeeRuleRequest windRequest() {
        return new WindFeeRuleRequest(VehicleType.BIKE, 10.0, 20.0, true, true, AMOUNT, false);
    }

    private PhenomenonFeeRuleRequest phenomenonRequest() {
        return new PhenomenonFeeRuleRequest(VehicleType.BIKE, PhenomenonCategory.RAIN, AMOUNT, false);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting to release the transaction");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }
}
