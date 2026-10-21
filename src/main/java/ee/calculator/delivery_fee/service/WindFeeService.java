package ee.calculator.delivery_fee.service;


import ee.calculator.delivery_fee.dto.WindFeeRuleRequest;
import ee.calculator.delivery_fee.entity.WindFeeRule;
import ee.calculator.delivery_fee.exception.FeeRuleConflictException;
import ee.calculator.delivery_fee.exception.FeeRuleNotFoundException;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.exception.VehicleForbiddenException;
import ee.calculator.delivery_fee.repository.WindFeeRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WindFeeService {

    private final WindFeeRuleRepository repository;
    private final Clock clock;
    private final FeeRuleWriteLock writeLock;

    /**
     * Returns the wind surcharge valid at the requested time,
     * or zero when no rule applies.
     *
     * @throws VehicleForbiddenException if the matching rule prohibits the vehicle
     */
    @Transactional(readOnly = true)
    public BigDecimal getAmount(
            VehicleType vehicleType,
            double windSpeed,
            Instant time
    ) {
        return repository.findApplicableRule(vehicleType, windSpeed, time)
                .map(rule -> {
                    if (rule.isForbidden()) {
                        throw new VehicleForbiddenException();
                    }
                    return rule.getAmount();
                })
                .orElse(BigDecimal.ZERO);
    }

    /** Lists all active and historical rule versions. */
    @Transactional(readOnly = true)
    public List<WindFeeRule> findAll() {
        return repository.findAll();
    }

    /** Returns a rule version by its identifier. */
    @Transactional(readOnly = true)
    public WindFeeRule findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new FeeRuleNotFoundException(id));
    }

    /** Creates a rule effective immediately, rejecting conflicting active rules. */
    @Transactional
    public WindFeeRule create(WindFeeRuleRequest request) {
        writeLock.acquire();
        Instant time = clock.instant().truncatedTo(ChronoUnit.MICROS);
        checkConflict(request, time, null);
        return saveNewRule(request, time);
    }

    /** Replaces an active rule without changing historical calculations. */
    @Transactional
    public WindFeeRule update(Long id, WindFeeRuleRequest request) {
        writeLock.acquire();
        Instant time = clock.instant().truncatedTo(ChronoUnit.MICROS);
        WindFeeRule existing = findEditableRule(id, time);
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
        WindFeeRule existing = findEditableRule(id, time);
        existing.setValidTo(time);
        repository.save(existing);
    }

    private WindFeeRule findEditableRule(Long id, Instant time) {
        WindFeeRule rule = repository.findById(id)
                .orElseThrow(() -> new FeeRuleNotFoundException(id));
        if (rule.getValidTo() != null) {
            throw new FeeRuleConflictException("Historical fee versions cannot be changed");
        }
        if (!time.isAfter(rule.getValidFrom())) {
            throw new FeeRuleConflictException("The change time must be after the rule's start time");
        }
        return rule;
    }

    private void checkConflict(WindFeeRuleRequest request, Instant time, Long excludedId) {
        boolean overlap = repository.findActiveRules(request.vehicleType(), time).stream()
                .filter(rule -> !rule.getId().equals(excludedId))
                .anyMatch(rule -> overlaps(rule, request));
        if (overlap) {
            throw new FeeRuleConflictException("An active rule overlaps the requested range");
        }
    }

    private boolean overlaps(WindFeeRule rule, WindFeeRuleRequest request) {
        boolean before = rule.getMaxWindSpeed() != null && request.minWindSpeed() != null
                && (rule.getMaxWindSpeed() < request.minWindSpeed()
                    || (rule.getMaxWindSpeed().doubleValue() == request.minWindSpeed().doubleValue()
                        && !(rule.isMaxInclusive() && request.minInclusive())));
        boolean after = request.maxWindSpeed() != null && rule.getMinWindSpeed() != null
                && (request.maxWindSpeed() < rule.getMinWindSpeed()
                    || (request.maxWindSpeed().doubleValue() == rule.getMinWindSpeed().doubleValue()
                        && !(request.maxInclusive() && rule.isMinInclusive())));
        return !before && !after;
    }

    private WindFeeRule saveNewRule(WindFeeRuleRequest request, Instant time) {
        WindFeeRule rule = new WindFeeRule();
        rule.setVehicleType(request.vehicleType());
        rule.setMinWindSpeed(request.minWindSpeed());
        rule.setMaxWindSpeed(request.maxWindSpeed());
        rule.setMinInclusive(request.minInclusive());
        rule.setMaxInclusive(request.maxInclusive());
        rule.setAmount(request.amount());
        rule.setForbidden(request.forbidden());
        rule.setValidFrom(time);
        return repository.save(rule);
    }
}
