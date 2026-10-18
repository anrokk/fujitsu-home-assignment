package ee.calculator.delivery_fee.entity;

import java.time.Instant;

import ee.calculator.delivery_fee.domain.City;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "weather_observation")
@Data
@NoArgsConstructor 
public class WeatherObservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private City city;

    @Column(name = "station_name", nullable = false, length = 100)
    private String stationName;

    @Column(name = "wmo_code", nullable = false, length = 10)
    private String wmoCode;

    @Column(name = "air_temperature")
    private Double airTemperature;

    @Column(name = "wind_speed")
    private Double windSpeed;

    @Column(length = 100)
    private String phenomenon;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;
}
