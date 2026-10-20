INSERT INTO temperature_fee_rule (
         vehicle_type,
         min_temperature,
         max_temperature,
         min_inclusive,
         max_inclusive,
         amount,
         valid_from
) VALUES
      ('SCOOTER', NULL, -10, FALSE, FALSE, 1.00,
       TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00:00'),
      ('SCOOTER', -10, 0, TRUE, TRUE, 0.50,
       TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00:00'),
      ('BIKE', NULL, -10, FALSE, FALSE, 1.00,
       TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00:00'),

      ('BIKE', -10, 0, TRUE, TRUE, 0.50,
       TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00:00');
