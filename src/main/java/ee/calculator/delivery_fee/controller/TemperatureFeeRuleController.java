package ee.calculator.delivery_fee.controller;

import ee.calculator.delivery_fee.dto.TemperatureFeeRuleRequest;
import ee.calculator.delivery_fee.dto.TemperatureFeeRuleResponse;
import ee.calculator.delivery_fee.entity.TemperatureFeeRule;
import ee.calculator.delivery_fee.service.TemperatureFeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/temperature-fee-rules")
@RequiredArgsConstructor
public class TemperatureFeeRuleController {

    private final TemperatureFeeService service;

    /** Lists active and historical rule versions. */
    @GetMapping
    public List<TemperatureFeeRuleResponse> findAll() {
        return service.findAll().stream().map(TemperatureFeeRuleResponse::from).toList();
    }

    /** Returns one rule version by its identifier. */
    @GetMapping("/{id}")
    public TemperatureFeeRuleResponse findById(@PathVariable("id") Long id) {
        return TemperatureFeeRuleResponse.from(service.findById(id));
    }

    /** Creates a rule effective immediately. */
    @PostMapping
    public ResponseEntity<TemperatureFeeRuleResponse> create(
            @Valid @RequestBody TemperatureFeeRuleRequest request) {
        TemperatureFeeRule created = service.create(request);
        return ResponseEntity.created(URI.create("/api/temperature-fee-rules/" + created.getId()))
                .body(TemperatureFeeRuleResponse.from(created));
    }

    /** Replaces an active rule and returns its new version and identifier. */
    @PutMapping("/{id}")
    public TemperatureFeeRuleResponse update(
            @PathVariable("id") Long id,
            @Valid @RequestBody TemperatureFeeRuleRequest request) {
        return TemperatureFeeRuleResponse.from(service.update(id, request));
    }

    /** Retires an active rule while preserving its history. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
