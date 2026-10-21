package ee.calculator.delivery_fee.service;


import ee.calculator.delivery_fee.dto.TemperatureFeeRuleRequest;
import ee.calculator.delivery_fee.exception.FeeRuleConflictException;
import ee.calculator.delivery_fee.exception.FeeRuleNotFoundException;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.TemperatureFeeRule;
import ee.calculator.delivery_fee.repository.TemperatureFeeRuleRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TemperatureFeeService {
    private final TemperatureFeeRuleRepository repository;
    private final Clock clock;
    private final FeeRuleWriteLock writeLock;


    /**
     * Returns the temperature surcharge valid at the requested time,
     * or zero when no rule applies.
     */
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

    /** Lists all active and historical rule versions. */
    @Transactional(readOnly = true)
    public List<TemperatureFeeRule> findAll() {
        return repository.findAll();
    }

    /** Returns a rule version by its identifier. */
    @Transactional(readOnly = true)
    public TemperatureFeeRule findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new FeeRuleNotFoundException(id));
    }

    /** Creates a rule effective immediately, rejecting conflicting active rules. */
    @Transactional
    public TemperatureFeeRule create(TemperatureFeeRuleRequest request) {
        writeLock.acquire();
        Instant time = clock.instant().truncatedTo(ChronoUnit.MICROS);
        checkConflict(request, time, null);
        return saveNewRule(request, time);
    }

    /** Replaces an active rule without changing historical calculations. */
    @Transactional
    public TemperatureFeeRule update(Long id, TemperatureFeeRuleRequest request) {
        writeLock.acquire();
        Instant time = clock.instant().truncatedTo(ChronoUnit.MICROS);
        TemperatureFeeRule existing = findEditableRule(id, time);
        checkConflict(request, time, id);
        existing.setValidTo(time);
        repository.save(existing);
        return saveNewRule(request, time);
    }

    /** Retires an active rule while preserving its historical version. */
    @Transactional
    public void delete(Long id) {
        writeLock.acquire();
        Instant time = clock.instant().truncatedTo(ChronoUnit.MICROS);
        TemperatureFeeRule existing = findEditableRule(id, time);
        existing.setValidTo(time);
        repository.save(existing);
    }

    private TemperatureFeeRule findEditableRule(Long id, Instant time) {
        TemperatureFeeRule rule = repository.findById(id)
                .orElseThrow(() -> new FeeRuleNotFoundException(id));
        if (rule.getValidTo() != null) {
            throw new FeeRuleConflictException("Historical fee versions cannot be changed");
        }
        if (!time.isAfter(rule.getValidFrom())) {
            throw new FeeRuleConflictException("The change time must be after the rule's start time");
        }
        return rule;
    }

    private void checkConflict(TemperatureFeeRuleRequest request, Instant time, Long excludedId) {
        boolean overlap = repository.findActiveRules(request.vehicleType(), time).stream()
                .filter(rule -> !rule.getId().equals(excludedId))
                .anyMatch(rule -> overlaps(rule, request));
        if (overlap) {
            throw new FeeRuleConflictException("An active rule overlaps the requested range");
        }
    }

    private boolean overlaps(TemperatureFeeRule rule, TemperatureFeeRuleRequest request) {
        boolean before = rule.getMaxTemperature() != null && request.minTemperature() != null
                && (rule.getMaxTemperature() < request.minTemperature()
                    || (rule.getMaxTemperature().doubleValue() == request.minTemperature().doubleValue()
                        && !(rule.isMaxInclusive() && request.minInclusive())));
        boolean after = request.maxTemperature() != null && rule.getMinTemperature() != null
                && (request.maxTemperature() < rule.getMinTemperature()
                    || (request.maxTemperature().doubleValue() == rule.getMinTemperature().doubleValue()
                        && !(request.maxInclusive() && rule.isMinInclusive())));
        return !before && !after;
    }

    private TemperatureFeeRule saveNewRule(TemperatureFeeRuleRequest request, Instant time) {
        TemperatureFeeRule rule = new TemperatureFeeRule();
        rule.setVehicleType(request.vehicleType());
        rule.setMinTemperature(request.minTemperature());
        rule.setMaxTemperature(request.maxTemperature());
        rule.setMinInclusive(request.minInclusive());
        rule.setMaxInclusive(request.maxInclusive());
        rule.setAmount(request.amount());
        rule.setValidFrom(time);
        return repository.save(rule);
    }
}
