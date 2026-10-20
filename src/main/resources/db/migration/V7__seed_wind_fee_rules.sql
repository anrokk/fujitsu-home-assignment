INSERT INTO wind_fee_rule (
    vehicle_type,
    min_wind_speed,
    max_wind_speed,
    min_inclusive,
    max_inclusive,
    amount,
    forbidden,
    valid_from
)
VALUES
    ('BIKE', 10, 20, TRUE, TRUE, 0.50, FALSE,
     TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00:00'),

    ('BIKE', 20, NULL, FALSE, FALSE, NULL, TRUE,
     TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00:00');