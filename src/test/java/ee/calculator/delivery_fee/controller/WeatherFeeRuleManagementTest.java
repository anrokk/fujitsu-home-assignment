package ee.calculator.delivery_fee.controller;

import com.jayway.jsonpath.JsonPath;
import ee.calculator.delivery_fee.domain.PhenomenonCategory;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.exception.VehicleForbiddenException;
import ee.calculator.delivery_fee.repository.PhenomenonFeeRuleRepository;
import ee.calculator.delivery_fee.repository.TemperatureFeeRuleRepository;
import ee.calculator.delivery_fee.repository.WindFeeRuleRepository;
import ee.calculator.delivery_fee.service.PhenomenonFeeService;
import ee.calculator.delivery_fee.service.TemperatureFeeService;
import ee.calculator.delivery_fee.service.WindFeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:weather_rule_management_test")
@AutoConfigureMockMvc
@Transactional
class WeatherFeeRuleManagementTest {

    private static final Instant TIME = Instant.parse("2026-10-20T18:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private TemperatureFeeRuleRepository temperatureRepository;
    @Autowired private WindFeeRuleRepository windRepository;
    @Autowired private PhenomenonFeeRuleRepository phenomenonRepository;
    @Autowired private TemperatureFeeService temperatureService;
    @Autowired private WindFeeService windService;
    @Autowired private PhenomenonFeeService phenomenonService;
    @MockitoBean private Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(TIME);
    }

    @ParameterizedTest
    @ValueSource(strings = {"temperature", "wind", "phenomenon"})
    void crudPreservesHistoricalCalculations(String kind) throws Exception {
        String url = url(kind);
        long originalId = originalId(kind);
        BigDecimal originalAmount = amount(kind, TIME.minusSeconds(1));

        mockMvc.perform(get(url)).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        mockMvc.perform(get(url + "/" + originalId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(originalId));

        String replacement = mockMvc.perform(put(url + "/" + originalId)
                        .contentType(MediaType.APPLICATION_JSON).content(body(kind, "3.25", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(3.25))
                .andExpect(jsonPath("$.validFrom").value(TIME.toString()))
                .andReturn().getResponse().getContentAsString();
        long replacementId = ((Number) JsonPath.read(replacement, "$.id")).longValue();

        assertThat(replacementId).isNotEqualTo(originalId);
        assertThat(amount(kind, TIME.minusSeconds(1))).isEqualByComparingTo(originalAmount);
        assertThat(amount(kind, TIME)).isEqualByComparingTo("3.25");
        mockMvc.perform(get(url + "/" + originalId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.validTo").value(TIME.toString()));

        Instant deletionTime = TIME.plusSeconds(60);
        when(clock.instant()).thenReturn(deletionTime);
        mockMvc.perform(delete(url + "/" + replacementId))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        assertThat(amount(kind, deletionTime.minusSeconds(1))).isEqualByComparingTo("3.25");
        assertThat(amount(kind, deletionTime)).isEqualByComparingTo("0");
        mockMvc.perform(get(url + "/" + replacementId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.validTo").value(deletionTime.toString()));

        Instant recreationTime = TIME.plusSeconds(120);
        when(clock.instant()).thenReturn(recreationTime);
        String created = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON)
                        .content(body(kind, "2.75", false)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.validFrom").value(recreationTime.toString()))
                .andReturn().getResponse().getContentAsString();
        long createdId = ((Number) JsonPath.read(created, "$.id")).longValue();
        mockMvc.perform(get(url + "/" + createdId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(2.75));
        assertThat(amount(kind, recreationTime)).isEqualByComparingTo("2.75");
        assertThat(amount(kind, TIME.minusSeconds(1))).isEqualByComparingTo(originalAmount);

        mockMvc.perform(put(url + "/" + originalId).contentType(MediaType.APPLICATION_JSON)
                        .content(body(kind, "4.00", false)))
                .andExpect(status().isConflict());
        mockMvc.perform(delete(url + "/" + originalId)).andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = {"temperature", "wind", "phenomenon"})
    void rejectsDuplicateActiveRules(String kind) throws Exception {
        mockMvc.perform(post(url(kind)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(kind, "2.00", false)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }

    @ParameterizedTest
    @ValueSource(strings = {"temperature", "wind", "phenomenon"})
    void rejectsConflictingUpdateWithoutChangingPrices(String kind) throws Exception {
        BigDecimal original = amount(kind, TIME);
        String conflictingBody = switch (kind) {
            case "temperature" -> body(kind, "3.00", false).replace("\"maxTemperature\":-10", "\"maxTemperature\":-5");
            case "wind" -> body(kind, "3.00", false).replace("\"maxWindSpeed\":20", "\"maxWindSpeed\":30");
            default -> body(kind, "3.00", false).replace("SNOW", "RAIN");
        };
        mockMvc.perform(put(url(kind) + "/" + originalId(kind))
                        .contentType(MediaType.APPLICATION_JSON).content(conflictingBody))
                .andExpect(status().isConflict());
        assertThat(amount(kind, TIME)).isEqualByComparingTo(original);
    }

    @ParameterizedTest
    @ValueSource(strings = {"temperature", "wind", "phenomenon"})
    void returns404ForUnknownRule(String kind) throws Exception {
        mockMvc.perform(get(url(kind) + "/-1")).andExpect(status().isNotFound());
        mockMvc.perform(put(url(kind) + "/-1").contentType(MediaType.APPLICATION_JSON)
                        .content(body(kind, "1.00", false))).andExpect(status().isNotFound());
        mockMvc.perform(delete(url(kind) + "/-1")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"wind", "phenomenon"})
    void changingToForbiddenPreservesEarlierSurcharge(String kind) throws Exception {
        BigDecimal original = amount(kind, TIME.minusSeconds(1));
        mockMvc.perform(put(url(kind) + "/" + originalId(kind))
                        .contentType(MediaType.APPLICATION_JSON).content(body(kind, "null", true)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.forbidden").value(true));
        assertThat(amount(kind, TIME.minusSeconds(1))).isEqualByComparingTo(original);
        assertThatThrownBy(() -> amount(kind, TIME)).isInstanceOf(VehicleForbiddenException.class);
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void rejectsInvalidDefinitions(String kind, String body) throws Exception {
        mockMvc.perform(post(url(kind)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(put(url(kind) + "/" + originalId(kind))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"temperature", "wind"})
    void sharedBoundaryConflictsOnlyWhenBothRangesIncludeIt(String kind) throws Exception {
        String adjacent = kind.equals("temperature")
                ? "{\"vehicleType\":\"BIKE\",\"minTemperature\":0,\"maxTemperature\":5,\"minInclusive\":true,\"maxInclusive\":true,\"amount\":0.25}"
                : "{\"vehicleType\":\"BIKE\",\"minWindSpeed\":0,\"maxWindSpeed\":10,\"minInclusive\":true,\"maxInclusive\":true,\"amount\":0.25,\"forbidden\":false}";
        mockMvc.perform(post(url(kind)).contentType(MediaType.APPLICATION_JSON).content(adjacent))
                .andExpect(status().isConflict());
        String allowed = kind.equals("temperature")
                ? adjacent.replace("\"minInclusive\":true", "\"minInclusive\":false")
                : adjacent.replace("\"maxInclusive\":true", "\"maxInclusive\":false");
        mockMvc.perform(post(url(kind)).contentType(MediaType.APPLICATION_JSON).content(allowed))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"));
    }

    private static Stream<Arguments> invalidRequests() {
        String temperature = body("temperature", "1.00", false);
        String wind = body("wind", "1.00", false);
        String phenomenon = body("phenomenon", "1.00", false);
        return Stream.of(
                Arguments.of("temperature", body("temperature", "null", false)),
                Arguments.of("temperature", body("temperature", "-1.00", false)),
                Arguments.of("temperature", body("temperature", "1.001", false)),
                Arguments.of("temperature", body("temperature", "100000000.00", false)),
                Arguments.of("temperature", temperature.replace("BIKE", "CAR")),
                Arguments.of("temperature", temperature.replace("\"maxTemperature\":-10", "\"maxTemperature\":null")),
                Arguments.of("temperature", temperature.replace("\"minTemperature\":null", "\"minTemperature\":-5")),
                Arguments.of("temperature", temperature.replace("\"minTemperature\":null", "\"minTemperature\":1e309")),
                Arguments.of("temperature", temperature.replace("\"minInclusive\":false", "\"minInclusive\":null")),
                Arguments.of("wind", wind.replace("BIKE", "SCOOTER")),
                Arguments.of("wind", wind.replace("\"minWindSpeed\":10", "\"minWindSpeed\":-1")),
                Arguments.of("wind", wind.replace("\"minWindSpeed\":10", "\"minWindSpeed\":20")),
                Arguments.of("wind", body("wind", "1.00", true)),
                Arguments.of("wind", body("wind", "null", false)),
                Arguments.of("wind", wind.replace("\"forbidden\":false", "\"forbidden\":null")),
                Arguments.of("phenomenon", body("phenomenon", "1.00", true)),
                Arguments.of("phenomenon", body("phenomenon", "null", false)),
                Arguments.of("phenomenon", phenomenon.replace("BIKE", "CAR")),
                Arguments.of("phenomenon", phenomenon.replace("\"phenomenonCategory\":\"SNOW\",", ""))
        );
    }

    private long originalId(String kind) {
        return switch (kind) {
            case "temperature" -> temperatureRepository.findApplicableRule(VehicleType.BIKE, -11, TIME).orElseThrow().getId();
            case "wind" -> windRepository.findApplicableRule(VehicleType.BIKE, 15, TIME).orElseThrow().getId();
            default -> phenomenonRepository.findApplicableRule(VehicleType.BIKE, PhenomenonCategory.SNOW, TIME).orElseThrow().getId();
        };
    }

    private BigDecimal amount(String kind, Instant time) {
        return switch (kind) {
            case "temperature" -> temperatureService.getAmount(VehicleType.BIKE, -11, time);
            case "wind" -> windService.getAmount(VehicleType.BIKE, 15, time);
            default -> phenomenonService.getAmount(VehicleType.BIKE, "Light snowfall", time);
        };
    }

    private static String body(String kind, String amount, boolean forbidden) {
        return switch (kind) {
            case "temperature" -> "{\"vehicleType\":\"BIKE\",\"minTemperature\":null,\"maxTemperature\":-10,\"minInclusive\":false,\"maxInclusive\":false,\"amount\":%s}".formatted(amount);
            case "wind" -> "{\"vehicleType\":\"BIKE\",\"minWindSpeed\":10,\"maxWindSpeed\":20,\"minInclusive\":true,\"maxInclusive\":true,\"amount\":%s,\"forbidden\":%s}".formatted(amount, forbidden);
            default -> "{\"vehicleType\":\"BIKE\",\"phenomenonCategory\":\"SNOW\",\"amount\":%s,\"forbidden\":%s}".formatted(amount, forbidden);
        };
    }

    private static String url(String kind) {
        return "/api/" + kind + "-fee-rules";
    }
}
