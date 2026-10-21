package ee.calculator.delivery_fee.service;


import ee.calculator.delivery_fee.domain.City;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.RegionalBaseFee;
import ee.calculator.delivery_fee.exception.FeeRuleConflictException;
import ee.calculator.delivery_fee.exception.FeeRuleNotFoundException;
import ee.calculator.delivery_fee.exception.RegionalBaseFeeNotFoundException;
import ee.calculator.delivery_fee.repository.RegionalBaseFeeRepository;
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
public class RegionalBaseFeeService {
    private final RegionalBaseFeeRepository repository;
    private final Clock clock;
    private final FeeRuleWriteLock writeLock;

    /**
     * Returns the base fee applicable at the requested time for the given city and vehicle type.
     */
    @Transactional(readOnly = true)
    public BigDecimal getAmount(
            City city,
            VehicleType vehicleType,
            Instant time
    ) {
        return repository.findApplicableFee(city, vehicleType, time)
                .orElseThrow(() -> new RegionalBaseFeeNotFoundException(
                        city,
                        vehicleType,
                        time
                ))
                .getAmount();
    }

    /**
     * Returns all base-fee versions, including historical versions.
     */
    @Transactional(readOnly = true)
    public List<RegionalBaseFee> findAll() {
        return repository.findAll();
    }

    /**
     * Returns a base-fee version by its identifier.
     */
    @Transactional(readOnly = true)
    public RegionalBaseFee findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new FeeRuleNotFoundException(id));
    }

    /**
     * Creates a base fee effective immediately.
     * An existing applicable fee must be updated instead.
     */
    @Transactional
    public RegionalBaseFee create(
            City city,
            VehicleType vehicleType,
            BigDecimal amount
    ) {
        writeLock.acquire();
        Instant time = clock.instant().truncatedTo(ChronoUnit.MICROS);

        if (repository.findApplicableFee(city, vehicleType, time).isPresent()) {
            throw new FeeRuleConflictException(
                    "An active base fee  already exists for %s and %s at %s"
                            .formatted(city, vehicleType, time)
            );
        }

        return saveNewFee(city, vehicleType, amount, time);
    }


    /**
     * Replaces an active fee's amount while preserving its history.
     * Returned replacement has a new identifier.
     */
    @Transactional
    public RegionalBaseFee update(Long id, BigDecimal amount) {
        writeLock.acquire();
        Instant time = clock.instant().truncatedTo(ChronoUnit.MICROS);
        RegionalBaseFee existingFee = findEditableFee(id, time);

        existingFee.setValidTo(time);
        repository.save(existingFee);

        return saveNewFee(
                existingFee.getCity(),
                existingFee.getVehicleType(),
                amount,
                time
        );
    }


    /**
     * Retires an active fee while preserving historical calculations.
     */
    @Transactional
    public void delete(Long id) {
        writeLock.acquire();
        Instant time = clock.instant().truncatedTo(ChronoUnit.MICROS);
        RegionalBaseFee existingFee = findEditableFee(id, time);

        existingFee.setValidTo(time);
        repository.save(existingFee);
    }

    private RegionalBaseFee findEditableFee(Long id, Instant time) {
        RegionalBaseFee fee = repository.findById(id)
                .orElseThrow(() -> new FeeRuleNotFoundException(id));

        if (fee.getValidTo() != null) {
            throw new FeeRuleConflictException(
                    "Historical fee versions cannot be changed");
        }

        if (!time.isAfter(fee.getValidFrom())) {
            throw new FeeRuleConflictException(
                    "The change time must be after the fee's start time"
            );
        }

        return fee;
    }

    private RegionalBaseFee saveNewFee(
            City city,
            VehicleType vehicleType,
            BigDecimal amount,
            Instant validFrom
    ) {
        RegionalBaseFee fee = new RegionalBaseFee();
        fee.setCity(city);
        fee.setVehicleType(vehicleType);
        fee.setAmount(amount);
        fee.setValidFrom(validFrom);

        return repository.save(fee);
    }
}
