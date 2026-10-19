package ee.calculator.delivery_fee.weather;


import ee.calculator.delivery_fee.entity.WeatherObservation;
import ee.calculator.delivery_fee.repository.WeatherObservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WeatherImportService {
    private final WeatherClient client;
    private final WeatherXmlParser parser;
    private final WeatherObservationRepository repository;


    public void importObservations() {
        String xml = client.fetchObservations();

        List<WeatherObservation> observations = parser.parse(xml);

        repository.saveAll(observations);
    }
}
