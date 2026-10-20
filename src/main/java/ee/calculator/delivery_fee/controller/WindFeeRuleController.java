package ee.calculator.delivery_fee.controller;

import ee.calculator.delivery_fee.dto.WindFeeRuleRequest;
import ee.calculator.delivery_fee.dto.WindFeeRuleResponse;
import ee.calculator.delivery_fee.entity.WindFeeRule;
import ee.calculator.delivery_fee.service.WindFeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/wind-fee-rules")
@RequiredArgsConstructor
public class WindFeeRuleController {

    private final WindFeeService service;

    /** Lists active and historical rule versions. */
    @GetMapping
    public List<WindFeeRuleResponse> findAll() {
        return service.findAll().stream().map(WindFeeRuleResponse::from).toList();
    }

    /** Returns one rule version by its identifier. */
    @GetMapping("/{id}")
    public WindFeeRuleResponse findById(@PathVariable("id") Long id) {
        return WindFeeRuleResponse.from(service.findById(id));
    }

    /** Creates a rule effective immediately. */
    @PostMapping
    public ResponseEntity<WindFeeRuleResponse> create(
            @Valid @RequestBody WindFeeRuleRequest request) {
        WindFeeRule created = service.create(request);
        return ResponseEntity.created(URI.create("/api/wind-fee-rules/" + created.getId()))
                .body(WindFeeRuleResponse.from(created));
    }

    /** Replaces an active rule and returns its new version and identifier. */
    @PutMapping("/{id}")
    public WindFeeRuleResponse update(
            @PathVariable("id") Long id,
            @Valid @RequestBody WindFeeRuleRequest request) {
        return WindFeeRuleResponse.from(service.update(id, request));
    }

    /** Retires an active rule while preserving its history. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
