package ee.calculator.delivery_fee.weather;

import ee.calculator.delivery_fee.domain.City;
import ee.calculator.delivery_fee.entity.WeatherObservation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

public class WeatherXmlParserTest {

    private final WeatherXmlParser parser = new WeatherXmlParser();

    private static final String xml = """
            <observations timestamp="1792420200">
                <station>
                    <name>Tallinn-Harku</name>
                    <wmocode>26038</wmocode>
                    <airtemperature></airtemperature>
                    <windspeed></windspeed>
                </station>
                <station>
                    <name>Tartu-Tõravere</name>
                    <wmocode>26242</wmocode>
                    <airtemperature>-2.1</airtemperature>
                    <windspeed>4.7</windspeed>
                    <phenomenon>Clear</phenomenon>
                </station>
                <station>
                    <name>Pärnu</name>
                    <wmocode>26231</wmocode>
                    <airtemperature></airtemperature>
                    <windspeed></windspeed>
                </station>
                <station>
                    <name>Some other station</name>
                </station>
            </observations>
            """;

    @Test
    void keepsOnlyRequiredStations() {
        List<WeatherObservation> observations = parser.parse(xml);

        assertThat(observations)
                .extracting(WeatherObservation::getCity)
                .containsExactly(City.TALLINN, City.TARTU, City.PARNU);
    }


    @Test
    void readsMeasurementsAndObservationTime() {
        WeatherObservation tartu = parser.parse(xml).get(1);

        assertThat(tartu.getStationName()).isEqualTo("Tartu-Tõravere");
        assertThat(tartu.getWmoCode()).isEqualTo("26242");
        assertThat(tartu.getAirTemperature()).isEqualTo(-2.1);
        assertThat(tartu.getWindSpeed()).isEqualTo(4.7);
        assertThat(tartu.getPhenomenon()).isEqualTo("Clear");
        assertThat(tartu.getObservedAt())
                .isEqualTo(Instant.parse("2026-10-19T14:30:00Z"));
    }

    @Test
    void preservesMissingMeasurementsAsNull() {
        List<WeatherObservation> observations = parser.parse(xml);

        WeatherObservation tallinn = observations.get(0);
        WeatherObservation parnu = observations.get(2);

        assertThat(tallinn.getAirTemperature()).isNull();
        assertThat(tallinn.getWindSpeed()).isNull();
        assertThat(parnu.getAirTemperature()).isNull();
        assertThat(parnu.getWindSpeed()).isNull();
    }

    @Test
    void rejectsMalformedXml() {
        assertThatThrownBy(() -> parser.parse("<observations>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Could not parse weather XML");
    }
}
