package ee.calculator.delivery_fee.controller;

import ee.calculator.delivery_fee.dto.PhenomenonFeeRuleRequest;
import ee.calculator.delivery_fee.dto.PhenomenonFeeRuleResponse;
import ee.calculator.delivery_fee.entity.PhenomenonFeeRule;
import ee.calculator.delivery_fee.service.PhenomenonFeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/phenomenon-fee-rules")
@RequiredArgsConstructor
public class PhenomenonFeeRuleController {

    private final PhenomenonFeeService service;

    /** Lists active and historical rule versions. */
    @GetMapping
    public List<PhenomenonFeeRuleResponse> findAll() {
        return service.findAll().stream().map(PhenomenonFeeRuleResponse::from).toList();
    }

    /** Returns one rule version by its identifier. */
    @GetMapping("/{id}")
    public PhenomenonFeeRuleResponse findById(@PathVariable("id") Long id) {
        return PhenomenonFeeRuleResponse.from(service.findById(id));
    }

    /** Creates a rule effective immediately. */
    @PostMapping
    public ResponseEntity<PhenomenonFeeRuleResponse> create(
            @Valid @RequestBody PhenomenonFeeRuleRequest request) {
        PhenomenonFeeRule created = service.create(request);
        return ResponseEntity.created(URI.create("/api/phenomenon-fee-rules/" + created.getId()))
                .body(PhenomenonFeeRuleResponse.from(created));
    }

    /** Replaces an active rule and returns its new version and identifier. */
    @PutMapping("/{id}")
    public PhenomenonFeeRuleResponse update(
            @PathVariable("id") Long id,
            @Valid @RequestBody PhenomenonFeeRuleRequest request) {
        return PhenomenonFeeRuleResponse.from(service.update(id, request));
    }

    /** Retires an active rule while preserving its history. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
