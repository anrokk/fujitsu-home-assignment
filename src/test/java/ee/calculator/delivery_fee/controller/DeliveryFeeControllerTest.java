package ee.calculator.delivery_fee.controller;


import ee.calculator.delivery_fee.domain.City;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.exception.RegionalBaseFeeNotFoundException;
import ee.calculator.delivery_fee.exception.VehicleForbiddenException;
import ee.calculator.delivery_fee.exception.WeatherDataUnavailableException;
import ee.calculator.delivery_fee.service.DeliveryFeeService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeliveryFeeController.class)
public class DeliveryFeeControllerTest {
    private static final Instant TIME = Instant.parse("2026-10-20T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeliveryFeeService service;

    @MockitoBean
    private Clock clock;

    @Test
    void usesRequestedDateTime() throws Exception {
        when(service.calculate(City.TARTU, VehicleType.BIKE, TIME))
                .thenReturn(new BigDecimal("4.00"));

        mockMvc.perform(get("/api/delivery-fees")
                        .param("city", "TARTU")
                        .param("vehicleType", "BIKE")
                        .param("datetime", TIME.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalFee").value(4.00));

        verify(service).calculate(City.TARTU, VehicleType.BIKE, TIME);
        verifyNoInteractions(clock);
    }

    @Test
    void usesCurrentTimeWhenDatetimeIsOmitted() throws Exception {
        when(clock.instant()).thenReturn(TIME);

        when(service.calculate(City.TALLINN, VehicleType.CAR, TIME))
                .thenReturn(new BigDecimal("4.00"));

        mockMvc.perform(get("/api/delivery-fees")
                        .param("city", "TALLINN")
                        .param("vehicleType", "CAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFee").value(4.00));

        verify(service).calculate(City.TALLINN, VehicleType.CAR, TIME);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "vehicleType=BIKE",
            "city=TARTU",
            "city=UNKNOWN&vehicleType=BIKE",
            "city=TARTU&vehicleType=UNKNOWN",
            "city=TARTU&vehicleType=BIKE&datetime=invalid"
    })
    void rejectsMissingOrInvalidParameters(String query) throws Exception {
        mockMvc.perform(get("/api/delivery-fees?" + query))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(service);
    }

    @Test
    void returns422WhenVehicleIsForbidden() throws Exception {
        when(service.calculate(City.TARTU, VehicleType.BIKE, TIME))
                .thenThrow(new VehicleForbiddenException());

        mockMvc.perform(get("/api/delivery-fees")
                        .param("city", "TARTU")
                        .param("vehicleType", "BIKE")
                        .param("datetime", TIME.toString()))
                .andExpect(status().is(422))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value(
                        "Usage of selected vehicle type is forbidden"));
    }

    @Test
    void returns404WhenBaseFeeIsMissing() throws Exception {
        var exception = new RegionalBaseFeeNotFoundException(
                City.TARTU, VehicleType.BIKE, TIME);

        when(service.calculate(City.TARTU, VehicleType.BIKE, TIME))
                .thenThrow(exception);

        mockMvc.perform(get("/api/delivery-fees")
                        .param("city", "TARTU")
                        .param("vehicleType", "BIKE")
                        .param("datetime", TIME.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(
                        exception.getMessage()));
    }

    @Test
    void returns503WhenWeatherIsUnavailable() throws Exception {
        when(service.calculate(City.TARTU, VehicleType.BIKE, TIME))
                .thenThrow(new WeatherDataUnavailableException(
                        "Wind speed is missing for TARTU"));

        mockMvc.perform(get("/api/delivery-fees")
                        .param("city", "TARTU")
                        .param("vehicleType", "BIKE")
                        .param("datetime", TIME.toString()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").value(
                        "Wind speed is missing for TARTU"));
    }
}
