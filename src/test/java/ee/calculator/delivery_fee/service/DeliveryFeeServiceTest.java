package ee.calculator.delivery_fee.service;


import ee.calculator.delivery_fee.domain.City;
import ee.calculator.delivery_fee.domain.VehicleType;
import ee.calculator.delivery_fee.entity.WeatherObservation;
import ee.calculator.delivery_fee.exception.VehicleForbiddenException;
import ee.calculator.delivery_fee.exception.WeatherDataUnavailableException;
import ee.calculator.delivery_fee.repository.WeatherObservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:delivery_fee_service_test")
@Transactional
public class DeliveryFeeServiceTest {
    private static final Instant TIME =
            Instant.parse("2026-10-01T00:00:00Z");

    @Autowired
    private DeliveryFeeService service;

    @Autowired
    private WeatherObservationRepository weatherRepository;

    @ParameterizedTest
    @CsvSource({
            "TARTU,   BIKE,    -2.1,  4.7, Light snow shower, 4.00",
            "TALLINN, BIKE,   -11.0, 10.0, Heavy snowfall,    5.50",
            "PARNU,   SCOOTER,  1.0, 30.0, Light rain,        3.00"
    })
    void combinesApplicableFees(
            City city,
            VehicleType vehicleType,
            double temperature,
            double windSpeed,
            String phenomenon,
            String expectedAmount
    ) {
        saveWeather(city, temperature, windSpeed, phenomenon, TIME);

        BigDecimal actualAmount = service.calculate(city, vehicleType, TIME);

        assertThat(actualAmount).isEqualByComparingTo(expectedAmount);
    }

    @Test
    void returnsCarBaseFeeWithoutWeather() {
        assertThat(service.calculate(
                City.TALLINN,
                VehicleType.CAR,
                TIME
        )).isEqualByComparingTo("4.00");
    }

    @Test
    void calculatesScooterFeeWithoutWindMeasurement() {
        saveWeather(City.TARTU, -2.1, null, "Clear", TIME);

        assertThat(service.calculate(
                City.TARTU,
                VehicleType.SCOOTER,
                TIME
        )).isEqualByComparingTo("3.50");
    }

    @Test
    void selectsLatestWeatherForCityAtOrBeforeRequestedTime() {
        saveWeather(
                City.TARTU, -20.0, 5.0, "Light snowfall",
                TIME.minusSeconds(7200)
        );

        saveWeather(
                City.TARTU, -2.1, 4.7, "Light snow shower",
                TIME.minusSeconds(3600)
        );

        saveWeather(
                City.TARTU, 10.0, 25.0, "Hail",
                TIME.plusSeconds(1)
        );

        saveWeather(City.TALLINN, 10.0, 5.0, "Clear", TIME);

        assertThat(service.calculate(
                City.TARTU,
                VehicleType.BIKE,
                TIME
        )).isEqualByComparingTo("4.00");
    }

    @ParameterizedTest
    @CsvSource({
            "BIKE,    20.1, Clear",
            "BIKE,     5.0, Hail",
            "SCOOTER, 25.0, Thunderstorm with rain"
    })
    void propagatesVehicleProhibition(
            VehicleType vehicleType,
            double windSpeed,
            String phenomenon
    ) {
        saveWeather(City.TARTU, 10.0, windSpeed, phenomenon, TIME);

        assertThatThrownBy(() -> service.calculate(
                City.TARTU,
                vehicleType,
                TIME
        ))
                .isInstanceOf(VehicleForbiddenException.class)
                .hasMessage("Usage of selected vehicle type is forbidden");
    }

    @Test
    void rejectsCalculationWithoutWeather() {
        assertThatThrownBy(() -> service.calculate(
                City.TARTU,
                VehicleType.BIKE,
                TIME
        )).isInstanceOf(WeatherDataUnavailableException.class);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "BIKE,    NULL, 5.0",
            "SCOOTER, NULL, 5.0",
            "BIKE,     5.0, NULL"
    }, nullValues = "NULL")
    void rejectsMissingRequiredMeasurements(
            VehicleType vehicleType,
            Double temperature,
            Double windSpeed
    ) {
        saveWeather(City.TARTU, temperature, windSpeed, "Clear", TIME);

        assertThatThrownBy(() -> service.calculate(
                City.TARTU,
                vehicleType,
                TIME
        )).isInstanceOf(WeatherDataUnavailableException.class);
    }


    private void saveWeather(
            City city,
            Double temperature,
            Double windSpeed,
            String phenomenon,
            Instant observedAt
    ) {
        WeatherObservation observation = new WeatherObservation();
        observation.setCity(city);
        observation.setStationName("Test station");
        observation.setWmoCode("12345");
        observation.setAirTemperature(temperature);
        observation.setWindSpeed(windSpeed);
        observation.setPhenomenon(phenomenon);
        observation.setObservedAt(observedAt);

        weatherRepository.save(observation);
    }
}
