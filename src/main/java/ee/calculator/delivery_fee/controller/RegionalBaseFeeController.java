package ee.calculator.delivery_fee.controller;


import ee.calculator.delivery_fee.dto.CreateRegionalBaseFeeRequest;
import ee.calculator.delivery_fee.dto.RegionalBaseFeeResponse;
import ee.calculator.delivery_fee.dto.UpdateRegionalBaseFeeRequest;
import ee.calculator.delivery_fee.entity.RegionalBaseFee;
import ee.calculator.delivery_fee.service.RegionalBaseFeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/regional-base-fees")
@RequiredArgsConstructor
public class RegionalBaseFeeController {

    private final RegionalBaseFeeService service;


    /**
     * Lists all base-fee versions, including historical versions.
     */
    @GetMapping
    public List<RegionalBaseFeeResponse> findAll() {
        return service.findAll().stream()
                .map(RegionalBaseFeeResponse::from)
                .toList();
    }


    /**
     * Returns a base-fee version by its identifier.
     */
    @GetMapping("/{id}")
    public RegionalBaseFeeResponse findById(
            @PathVariable Long id
    ) {
        return RegionalBaseFeeResponse.from(service.findById(id));
    }

    /**
     * Creates a base fee effective immediately
     */
    @PostMapping
    public ResponseEntity<RegionalBaseFeeResponse> create(
            @Valid @RequestBody CreateRegionalBaseFeeRequest request
    ) {
        RegionalBaseFee created = service.create(
                request.city(),
                request.vehicleType(),
                request.amount()
        );

        URI location = URI.create(
                "/api/regional-base-fees/" + created.getId());

        return ResponseEntity.created(location)
                .body(RegionalBaseFeeResponse.from(created));
    }

    /**
     * Replaces an active fee's amount and returns the new version.
     */
    @PutMapping("/{id}")
    public RegionalBaseFeeResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRegionalBaseFeeRequest request) {

        return RegionalBaseFeeResponse.from(
                service.update(id, request.amount()));
    }

    /**
     * Retires an active fee while retaining its historical version.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.noContent().build();
    }

}
