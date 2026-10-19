package ee.calculator.delivery_fee.weather;

import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class WeatherClient {
    private final RestClient restClient;
    private final String observationsUrl;

    public WeatherClient(
            RestClient.Builder builder,
            @Value("${weather.observations-url}") String observationsUrl) {
        this.restClient = builder.build();
        this.observationsUrl = observationsUrl;
    }

    public String fetchObservations() {
        String xml = restClient.get()
                .uri(observationsUrl)
                .accept(MediaType.APPLICATION_XML)
                .retrieve()
                .body(String.class);

        if (xml == null || xml.isBlank()) {
            throw new IllegalStateException("Weather service returned an empty response");
        }

        return xml;
    }
}
