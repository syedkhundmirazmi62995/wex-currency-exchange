# Currency Exchange Service

A small Spring Boot service that stores purchase transactions in US dollars and retrieves them
converted into another country's currency, using the U.S. Treasury
[Reporting Rates of Exchange](https://fiscaldata.treasury.gov/datasets/treasury-reporting-rates-exchange/treasury-reporting-rates-of-exchange)
dataset.

It does two things:

1. **Store a purchase** — a description, a transaction date, and a USD amount. The amount is rounded
   to the nearest cent and the purchase is assigned an id.
2. **Retrieve a purchase in a target currency** — the stored amount converted using the most recent
   exchange rate published on or before the transaction date, within the previous six months. If no
   such rate exists, the conversion is rejected.

## Requirements

- A JDK 21 (or newer). Nothing else — the Maven Wrapper pulls in the right Maven version, and there's
  no database or other infrastructure to stand up.

## Running it

```bash
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080`. An interactive API explorer is available at
`http://localhost:8080/swagger-ui.html`.

## Using the API

Store a purchase:

```bash
curl -s -X POST http://localhost:8080/api/v1/purchases \
  -H 'Content-Type: application/json' \
  -d '{"description":"Office supplies","transactionDate":"2025-12-31","amount":"19.995"}'
```

```json
{
  "id": "8f2a6c1e-3b4d-4e5f-9a01-2c3d4e5f6a7b",
  "description": "Office supplies",
  "transactionDate": "2025-12-31",
  "amount": "20.00"
}
```

Retrieve it converted to Canadian dollars (use the `id` from the previous response):

```bash
curl -s "http://localhost:8080/api/v1/purchases/8f2a6c1e-3b4d-4e5f-9a01-2c3d4e5f6a7b/converted?currency=Canada-Dollar"
```

```json
{
  "id": "8f2a6c1e-3b4d-4e5f-9a01-2c3d4e5f6a7b",
  "description": "Office supplies",
  "transactionDate": "2025-12-31",
  "originalAmount": "20.00",
  "currency": "Canada-Dollar",
  "exchangeRate": "1.369",
  "convertedAmount": "27.38"
}
```

The `currency` value is the Treasury's "Country - Currency" descriptor, e.g. `Canada-Dollar`,
`Mexico-Peso`, or `Euro Zone-Euro`. Storing and reading a purchase work offline; converting one
requires network access to the Treasury API.

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/purchases` | Store a purchase. |
| `GET`  | `/api/v1/purchases/{id}` | Get a stored purchase. |
| `GET`  | `/api/v1/purchases/{id}/converted?currency={descriptor}` | Get a purchase converted to a currency. |

Errors share one shape:

```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "The purchase cannot be converted to the target currency 'Atlantis-Doubloon': no exchange rate on or before 2025-12-31 within the prior 6 months.",
  "fieldErrors": null,
  "timestamp": "2026-06-11T12:00:00Z"
}
```

`fieldErrors` is populated only for request-validation failures (`400`).

## Money handling

All amounts use `BigDecimal`; no `double` or `float` is involved at any point. Stored amounts are
rounded to two decimal places with `HALF_UP` ("round half away from zero"), the usual reading of
"nearest cent". A conversion multiplies the stored amount by the published rate at full precision and
rounds once, at the end. Monetary values are sent over the wire as JSON strings (`"20.00"`) so the
exact value and scale survive a consumer's JSON parser.

## How a rate is chosen

The Treasury publishes rates periodically. For a conversion we ask the API for rates matching the
currency with a `record_date` from `transactionDate - 6 months` up to and including the
`transactionDate`, sorted newest first, and take the single most recent one. If the result is empty —
which also happens for a currency the Treasury doesn't list — the purchase can't be converted and the
request fails with `422`.

## Assumptions

A few things the brief left open, and the calls made here:

- **Identifier** — a server-generated random UUID. The brief asks for a unique identifier assigned on
  store but doesn't specify a format.
- **Rounding** — `HALF_UP`. It's centralized in `MoneyMath`, so switching to banker's rounding would
  be a one-line change.
- **Future dates** — a transaction can't be dated in the future; such requests are rejected. The check
  uses a UTC clock so it behaves the same wherever the service runs.
- **Unknown currency** — returns `422` ("cannot be converted") rather than `400`. The Treasury returns
  an empty result for an unknown currency, which is indistinguishable from a currency that simply has
  no rate in the window, so both are treated the same.
- **Persistence** — in-memory only, per the "no external database" constraint. Stored purchases do not
  survive a restart.

## Tests

```bash
./mvnw verify
```

The whole suite runs offline — the Treasury endpoint is stubbed with MockWebServer, so no test
depends on the network. Coverage spans the rounding/validation rules, the rate-selection logic and
its six-month boundary, the Treasury query and its error translation (timeout, upstream error,
malformed payload), and the HTTP layer end to end.

There is one optional check, disabled by default, that calls the real Treasury API to confirm the
live contract still matches our assumptions:

```bash
./mvnw test -Dtest=TreasuryRatesLiveTest -Dlive.tests=true
```
