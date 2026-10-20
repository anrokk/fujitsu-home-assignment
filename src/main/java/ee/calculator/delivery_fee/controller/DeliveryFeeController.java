package ee.calculator.delivery_fee.controller;


import ee.calculator.delivery_fee.domain.City;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.dto.DeliveryFeeResponse;
import ee.calculator.delivery_fee.service.DeliveryFeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/api/delivery-fees")
@RequiredArgsConstructor
public class DeliveryFeeController {
    private final DeliveryFeeService service;
    private final Clock clock;

    @GetMapping
    public DeliveryFeeResponse calculate(
            @RequestParam("city") City city,
            @RequestParam("vehicleType")VehicleType vehicleType,
            @RequestParam(name = "datetime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant datetime
            ) {
        Instant time = datetime == null ? clock.instant() : datetime;

        return new DeliveryFeeResponse(
                service.calculate(city, vehicleType, time)
        );
    }
}
