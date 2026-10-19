package ee.calculator.delivery_fee.weather;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class WeatherClientTest {
    private static final String url = "https://weather.example/observations";

    private MockRestServiceServer server;
    private WeatherClient client;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new WeatherClient(builder, url);
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void returnsXmlFromWeatherService() {
        String xml = "<observations/>";

        server.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_XML_VALUE
                ))
                .andRespond(withSuccess(xml, MediaType.APPLICATION_XML));

        String result = client.fetchObservations();

        assertThat(result).isEqualTo(xml);
    }

    @Test
    void rejectsEmptyResponse() {
        server.expect(requestTo(url)).andRespond(withSuccess("", MediaType.APPLICATION_XML));

        assertThatThrownBy(() -> client.fetchObservations())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Weather service returned an empty response");
    }

    @Test
    void throwsWhenWeatherServiceReturnsServerError() {
        server.expect(requestTo(url))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.fetchObservations())
                .isInstanceOf(HttpServerErrorException.class);
    }

}
