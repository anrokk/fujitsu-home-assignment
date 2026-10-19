package ee.calculator.delivery_fee.repository;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ee.calculator.delivery_fee.domain.City;
import ee.calculator.delivery_fee.entity.WeatherObservation;
import jakarta.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest("spring.datasource.url=jdbc:h2:mem:weather_repository_test")
@Transactional
public class WeatherObservationRepositoryTest {
    @Autowired
    private WeatherObservationRepository repository;

    @Test
    void findsLatestObservationAtOrBeforeRequestedTime() {
        saveObservation(City.TARTU, "2026-10-19T08:00:00Z");

        WeatherObservation expected = saveObservation(City.TARTU, "2026-10-19T09:00:00Z");

        saveObservation(City.TARTU, "2026-10-19T10:00:00Z");
        saveObservation(City.TALLINN, "2026-10-19T09:15:00Z");

        WeatherObservation actual = (repository.findFirstByCityAndObservedAtLessThanEqualOrderByObservedAtDesc(
                City.TARTU, Instant.parse("2026-10-19T09:30:00Z")).orElseThrow());

        assertThat(actual.getId()).isEqualTo(expected.getId());
    }

    private WeatherObservation saveObservation(City city, String timestamp) {
        WeatherObservation observation = new WeatherObservation();
        observation.setCity(city);
        observation.setStationName("Test Station");
        observation.setWmoCode("12345");
        observation.setObservedAt(Instant.parse(timestamp));

        return repository.save(observation);
    }
}
