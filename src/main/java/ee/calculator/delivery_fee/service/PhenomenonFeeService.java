package ee.calculator.delivery_fee.service;


import ee.calculator.delivery_fee.dto.PhenomenonFeeRuleRequest;
import ee.calculator.delivery_fee.entity.PhenomenonFeeRule;
import ee.calculator.delivery_fee.exception.FeeRuleConflictException;
import ee.calculator.delivery_fee.exception.FeeRuleNotFoundException;
import ee.calculator.delivery_fee.domain.PhenomenonCategory;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.exception.VehicleForbiddenException;
import ee.calculator.delivery_fee.repository.PhenomenonFeeRuleRepository;
import ee.calculator.delivery_fee.weather.PhenomenonClassifier;
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
public class PhenomenonFeeService {
    private final PhenomenonFeeRuleRepository repository;
    private final Clock clock;
    private final PhenomenonClassifier classifier;

    /**
     * Classifies the phenomenon and returns its surcharge at the requested time,
     * or zero when no rule applies.
     *
     * @throws VehicleForbiddenException if the matching rule prohibits the vehicle
     */
    @Transactional(readOnly = true)
    public BigDecimal getAmount(
            VehicleType vehicleType,
            String phenomenon,
            Instant time
    ) {
        PhenomenonCategory category = classifier.classify(phenomenon);

        return repository.findApplicableRule(vehicleType, category, time)
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
    public List<PhenomenonFeeRule> findAll() {
        return repository.findAll();
    }

    /** Returns a rule version by its identifier. */
    @Transactional(readOnly = true)
    public PhenomenonFeeRule findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new FeeRuleNotFoundException(id));
    }

    /** Creates a rule effective immediately, rejecting conflicting active rules. */
    @Transactional
    public PhenomenonFeeRule create(PhenomenonFeeRuleRequest request) {
        Instant time = clock.instant().truncatedTo(ChronoUnit.MICROS);
        checkConflict(request, time, null);
        return saveNewRule(request, time);
    }

    /** Replaces an active rule without changing historical calculations. */
    @Transactional
    public PhenomenonFeeRule update(Long id, PhenomenonFeeRuleRequest request) {
        Instant time = clock.instant().truncatedTo(ChronoUnit.MICROS);
        PhenomenonFeeRule existing = findEditableRule(id, time);
        checkConflict(request, time, id);
        existing.setValidTo(time);
        repository.save(existing);
        return saveNewRule(request, time);
    }

    /** Retires an active rule while preserving its historical version. */
    @Transactional
    public void delete(Long id) {
        Instant time = clock.instant().truncatedTo(ChronoUnit.MICROS);
        PhenomenonFeeRule existing = findEditableRule(id, time);
        existing.setValidTo(time);
        repository.save(existing);
    }

    private PhenomenonFeeRule findEditableRule(Long id, Instant time) {
        PhenomenonFeeRule rule = repository.findById(id)
                .orElseThrow(() -> new FeeRuleNotFoundException(id));
        if (rule.getValidTo() != null) {
            throw new FeeRuleConflictException("Historical fee versions cannot be changed");
        }
        if (!time.isAfter(rule.getValidFrom())) {
            throw new FeeRuleConflictException("The change time must be after the rule's start time");
        }
        return rule;
    }

    private void checkConflict(PhenomenonFeeRuleRequest request, Instant time, Long excludedId) {
        boolean duplicate = repository.findApplicableRule(
                        request.vehicleType(), request.phenomenonCategory(), time)
                .filter(rule -> !rule.getId().equals(excludedId))
                .isPresent();
        if (duplicate) {
            throw new FeeRuleConflictException("An active rule already exists for this vehicle and phenomenon");
        }
    }

    private PhenomenonFeeRule saveNewRule(PhenomenonFeeRuleRequest request, Instant time) {
        PhenomenonFeeRule rule = new PhenomenonFeeRule();
        rule.setVehicleType(request.vehicleType());
        rule.setPhenomenonCategory(request.phenomenonCategory());
        rule.setAmount(request.amount());
        rule.setForbidden(request.forbidden());
        rule.setValidFrom(time);
        return repository.save(rule);
    }
}
