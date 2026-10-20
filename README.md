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

## Errors

Errors use JSON Problem Details, including status and detail fields.

| Status | Meaning |
|---|---|
| 400 | Missing or invalid request parameters or body |
| 404 | Requested fee version or applicable base fee not found |
| 409 | Duplicate active fee or invalid change to a fee version |
| 422 | Weather conditions prohibit the selected vehicle |
| 503 | Weather data needed for calculation is unavailable |

The prohibition message is:
`Usage of selected vehicle type is forbidden`

## Scope and limitations

Regional base fees support REST CRUD with historical versioning.

Temperature, wind, and phenomenon rules are stored in the database and
support historical calculations. Their REST management endpoints are
not implemented.

Concurrent pricing changes are not protected against overlapping versions.
Management changes take effect immediately; scheduled and backdated changes
are not supported.