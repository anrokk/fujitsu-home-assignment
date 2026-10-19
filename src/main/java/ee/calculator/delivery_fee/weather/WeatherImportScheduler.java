package ee.calculator.delivery_fee.weather;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherImportScheduler {
    private final WeatherImportService service;

    @Scheduled(
            cron = "${weather.import.cron}",
            zone = "${weather.import.zone}"
    )
    public void importWeatherData() {
        service.importObservations();
        log.info("Weather observations import completed successfully.");
    }
}
