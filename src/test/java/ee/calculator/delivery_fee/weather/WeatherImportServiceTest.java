package ee.calculator.delivery_fee.weather;


import ee.calculator.delivery_fee.entity.WeatherObservation;
import ee.calculator.delivery_fee.repository.WeatherObservationRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:weather_import_test"
)
@Transactional
public class WeatherImportServiceTest {
    @Autowired
    private WeatherImportService service;

    @Autowired
    private WeatherObservationRepository repository;

    @MockitoBean
    private WeatherClient client;

    private static final String xml = """
            <observations timestamp="1792420200">
                <station>
                    <name>Tallinn-Harku</name>
                    <wmocode>26038</wmocode>
                    <airtemperature>1.0</airtemperature>
                    <windspeed>2.0</windspeed>
                    <phenomenon>Clear</phenomenon>
                </station>
                <station>
                    <name>Tartu-Tõravere</name>
                    <wmocode>26242</wmocode>
                    <airtemperature>-2.1</airtemperature>
                    <windspeed>4.7</windspeed>
                    <phenomenon>Light snow</phenomenon>
                </station>
                <station>
                    <name>Pärnu</name>
                    <wmocode>26231</wmocode>
                    <airtemperature>3.0</airtemperature>
                    <windspeed>5.0</windspeed>
                    <phenomenon>Clear</phenomenon>
                </station>
            </observations>
            """;

    @Test
    void appendsObservationsWithoutRemovingHistory() {
        when(client.fetchObservations()).thenReturn(xml);

        service.importObservations();

        assertThat(repository.count()).isEqualTo(3);

        List<Long> originalIds = repository.findAll().stream()
                .map(WeatherObservation::getId)
                .toList();

        service.importObservations();

        assertThat(repository.count()).isEqualTo(6);

        assertThat(repository.findAll())
                .extracting(WeatherObservation::getId)
                .containsAll(originalIds);
    }
}
