package ee.calculator.delivery_fee.entity;


import ee.calculator.delivery_fee.domain.VehicleType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wind_fee_rule")
@Data
@NoArgsConstructor
public class WindFeeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @Column(name = "min_wind_speed")
    private Double minWindSpeed;

    @Column(name = "max_wind_speed")
    private Double maxWindSpeed;

    @Column(name = "min_inclusive", nullable = false)
    private boolean minInclusive;

    @Column(name = "max_inclusive", nullable = false)
    private boolean maxInclusive;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private boolean forbidden;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;
}
