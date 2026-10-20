package ee.calculator.delivery_fee.controller;

import ee.calculator.delivery_fee.domain.City;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.RegionalBaseFee;
import ee.calculator.delivery_fee.exception.FeeRuleConflictException;
import ee.calculator.delivery_fee.exception.FeeRuleNotFoundException;
import ee.calculator.delivery_fee.service.RegionalBaseFeeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegionalBaseFeeController.class)
class RegionalBaseFeeControllerTest {

    private static final String URL = "/api/regional-base-fees";
    private static final Instant TIME =
            Instant.parse("2026-10-20T17:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegionalBaseFeeService service;

    @Test
    void listsHistoricalVersions() throws Exception {
        RegionalBaseFee historical = fee(10L, "2.50");
        historical.setValidTo(TIME.plusSeconds(60));

        when(service.findAll()).thenReturn(List.of(historical));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].amount").value(2.50))
                .andExpect(jsonPath("$[0].validTo").value(
                        TIME.plusSeconds(60).toString()));
    }

    @Test
    void returnsFeeById() throws Exception {
        when(service.findById(10L)).thenReturn(fee(10L, "2.50"));

        mockMvc.perform(get(URL + "/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.city").value("TARTU"))
                .andExpect(jsonPath("$.vehicleType").value("BIKE"))
                .andExpect(jsonPath("$.amount").value(2.50))
                .andExpect(jsonPath("$.validFrom").value(TIME.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "3.25"})
    void createsFee(String amount) throws Exception {
        BigDecimal price = new BigDecimal(amount);

        when(service.create(City.TARTU, VehicleType.BIKE, price))
                .thenReturn(fee(10L, amount));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "city": "TARTU",
                                  "vehicleType": "BIKE",
                                  "amount": %s
                                }
                                """.formatted(amount)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", URL + "/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.amount").value(price.doubleValue()));

        verify(service).create(City.TARTU, VehicleType.BIKE, price);
    }

    @Test
    void updateReturnsReplacementVersion() throws Exception {
        when(service.update(10L, new BigDecimal("3.25")))
                .thenReturn(fee(11L, "3.25"));

        mockMvc.perform(put(URL + "/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 3.25}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.amount").value(3.25));

        verify(service).update(10L, new BigDecimal("3.25"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete(URL + "/10"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete(10L);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "null", "-0.01", "1.001", "100000000.00"
    })
    void rejectsInvalidCreateAmount(String amount) throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "city": "TARTU",
                                  "vehicleType": "BIKE",
                                  "amount": %s
                                }
                                """.formatted(amount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "null", "-0.01", "1.001", "100000000.00"
    })
    void rejectsInvalidUpdateAmount(String amount) throws Exception {
        mockMvc.perform(put(URL + "/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": %s}
                                """.formatted(amount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"vehicleType\":\"BIKE\",\"amount\":3.00}",
            "{\"city\":\"TARTU\",\"amount\":3.00}",
            "{\"city\":\"TARTU\",\"vehicleType\":\"BIKE\"}",
            "{\"city\":\"UNKNOWN\",\"vehicleType\":\"BIKE\",\"amount\":3.00}"
    })
    void rejectsMissingFieldsOrUnknownCity(String body) throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void returns404ForUnknownFee() throws Exception {
        when(service.findById(10L))
                .thenThrow(new FeeRuleNotFoundException(10L));

        mockMvc.perform(get(URL + "/10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(
                        "Fee rule not found: 10"));
    }

    @Test
    void returns409ForConflictingChange() throws Exception {
        when(service.update(10L, new BigDecimal("3.25")))
                .thenThrow(new FeeRuleConflictException(
                        "Historical fee versions cannot be changed"));

        mockMvc.perform(put(URL + "/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 3.25}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(
                        "Historical fee versions cannot be changed"));
    }

    private RegionalBaseFee fee(Long id, String amount) {
        RegionalBaseFee fee = new RegionalBaseFee();
        fee.setId(id);
        fee.setCity(City.TARTU);
        fee.setVehicleType(VehicleType.BIKE);
        fee.setAmount(new BigDecimal(amount));
        fee.setValidFrom(TIME);
        return fee;
    }
}