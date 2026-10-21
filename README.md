# Delivery Fee Calculator

Spring Boot application that calculates delivery fees from the city,
vehicle type, weather observations, and applicable pricing rules.

## Requirements

- Java 21
- Internet access for downloading dependencies and importing live weather

The Gradle wrapper is included; a separate Gradle installation is unnecessary.

## Run

```bash
./gradlew bootRun
```

The application runs at `http://localhost:8080`.

Flyway creates the database schema and seeds the pricing rules.
The H2 database persists locally under `data/`.

## Tests

```bash
./gradlew test
```

Tests use in-memory databases and mocked HTTP responses where applicable.

## Weather imports

Observations are imported for Tallinn-Harku, Tartu-Tõravere, and Pärnu.
Each import inserts new records and preserves previous observations.

The default schedule is once per hour at HH:15:00, Europe/Tallinn time.
Starting the application does not itself trigger an import.

To import every ten seconds during a local demonstration:

```bash
WEATHER_IMPORT_CRON='*/10 * * * * *' ./gradlew bootRun
```

Wait for the successful import log before requesting a weather-dependent
calculation. Historical requests require an observation at or before the
requested datetime.

Configuration is in `src/main/resources/application.properties`:

| Property | Purpose |
|---|---|
| `weather.import.cron` | Import schedule |
| `weather.import.zone` | Schedule timezone |
| `weather.observations-url` | Weather XML source |
| `spring.datasource.url` | Database connection |

## Delivery fee API

### GET /api/delivery-fees

| Parameter | Required | Accepted values |
|---|---|---|
| `city` | Yes | `TALLINN`, `TARTU`, `PARNU` |
| `vehicleType` | Yes | `CAR`, `SCOOTER`, `BIKE` |
| `datetime` | No | ISO 8601 instant, e.g. `2026-10-20T10:00:00Z` |

When datetime is omitted, the current time is used.
The calculation selects the latest observation at or before that time
and the pricing rules valid at that time.

Example:

```bash
curl 'http://localhost:8080/api/delivery-fees?city=TALLINN&vehicleType=CAR'
```

Response with the initial pricing rules:

```json
{
  "totalFee": 4.00
}
```

Amounts are in euros.

Cars use the regional base fee without requiring weather.
Scooters require temperature and phenomenon data.
Bikes additionally require wind-speed data.

Boundary interpretation:
- Temperature from -10°C through 0°C includes both endpoints.
- Wind speed from 10 through 20 m/s includes both endpoints.
- Validity periods include validFrom and exclude validTo.
- A null validTo means the version has no end time.
- An empty weather phenomenon produces no phenomenon surcharge.

## Regional base-fee management API

| Method | Path | Successful response |
|---|---|---|
| GET | `/api/regional-base-fees` | 200, array of all versions |
| GET | `/api/regional-base-fees/{id}` | 200, one version |
| POST | `/api/regional-base-fees` | 201, created version and Location header |
| PUT | `/api/regional-base-fees/{id}` | 200, replacement version |
| DELETE | `/api/regional-base-fees/{id}` | 204, empty body |

POST request body:

```json
{
  "city": "TARTU",
  "vehicleType": "BIKE",
  "amount": 3.00
}
```

PUT request body:

```json
{
  "amount": 3.25
}
```

POST and PUT require `Content-Type: application/json`.

Amounts must be nonnegative, with at most eight integer digits and
two decimal places. City and vehicle type are required for POST.

A fee response contains:
- `id`
- `city`
- `vehicleType`
- `amount`
- `validFrom`
- `validTo`

Changes take effect immediately. PUT closes the previous version and
returns a replacement with a new ID. DELETE retires the selected version
without removing its history.

Historical versions remain readable and cannot be changed.
POST rejects a city/vehicle combination that already has an active fee.
All nine city/vehicle combinations are initially seeded, so use PUT to
change their prices.

## Weather-rule management APIs

The following collections support the same five operations as base fees:
GET the collection, GET `/{id}`, POST, PUT `/{id}`, and DELETE `/{id}`.
Success statuses are 200, 200, 201, 200, and 204 respectively.

- `/api/temperature-fee-rules`
- `/api/wind-fee-rules`
- `/api/phenomenon-fee-rules`

POST and PUT require `Content-Type: application/json` and a complete rule
definition. IDs and validity timestamps are assigned by the application.
Responses contain the rule definition, `id`, `validFrom`, and `validTo`.
POST returns a Location header pointing to the created version.

Temperature request example:

```json
{
  "vehicleType": "BIKE",
  "minTemperature": -10,
  "maxTemperature": 0,
  "minInclusive": true,
  "maxInclusive": true,
  "amount": 0.75
}
```

Wind request example:

```json
{
  "vehicleType": "BIKE",
  "minWindSpeed": 20,
  "maxWindSpeed": null,
  "minInclusive": false,
  "maxInclusive": false,
  "amount": null,
  "forbidden": true
}
```

Phenomenon request example:

```json
{
  "vehicleType": "SCOOTER",
  "phenomenonCategory": "RAIN",
  "amount": 0.75,
  "forbidden": false
}
```

These examples match seeded rule identities or ranges; use PUT with the active
version's ID to change them. POST returns 409 if they conflict with active rules.

Temperature and phenomenon rules support SCOOTER and BIKE; wind rules support
BIKE. Phenomenon categories are SNOW, SLEET, RAIN, GLAZE, HAIL, THUNDER, and OTHER.

Range rules require explicit minInclusive and maxInclusive flags. At least one
bound must be present; a null bound is unbounded. Bounds must be finite and
minimum must be less than maximum. Wind bounds must also be nonnegative.
Active ranges for the same vehicle must not overlap, including shared endpoints
when both ranges include the endpoint. A vehicle and phenomenon category can
have only one active phenomenon rule.

Temperature rules require a nonnegative amount. Wind and phenomenon rules
require an explicit forbidden flag: forbidden rules require a null amount;
other rules require a nonnegative amount. All amounts use the same precision
limits as base fees.

PUT retires the previous version and returns a replacement with a new ID.
DELETE retires the active version. Historical versions remain readable and
cannot be edited or deleted. Earlier calculations retain the previous rules.
After retirement, an unmatched weather condition contributes no surcharge.

## Errors

Errors use JSON Problem Details, including status and detail fields.

| Status | Meaning |
|---|---|
| 400 | Missing or invalid request parameters or body |
| 404 | Requested fee version or applicable base fee not found |
| 409 | Duplicate or overlapping active rules, or invalid change to a historical version or at its start time |
| 422 | Weather conditions prohibit the selected vehicle |
| 503 | Weather data needed for calculation is unavailable |

The prohibition message is:
`Usage of selected vehicle type is forbidden`

## Scope and limitations

Regional base fees and all three weather-rule types support REST CRUD
with historical versioning.

Pricing changes lock the same database row before validating or changing rules.
The lock is held until commit or rollback, preventing concurrent writes from
creating overlapping versions. Pricing writes run one at a time; calculation
requests do not acquire this lock.
Management changes take effect immediately; scheduled and backdated changes
are not supported.
