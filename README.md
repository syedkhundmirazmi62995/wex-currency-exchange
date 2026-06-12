# Currency Exchange Service

A small web service that stores purchases made in US dollars and can show them in another
country's currency. The exchange rates come from the US Treasury.

## What you need

Java 21 or newer. There's no database to set up.

## Run it

```bash
./mvnw spring-boot:run
```

It starts on `http://localhost:8080`. You can try the API from your browser at
`http://localhost:8080/swagger-ui.html`.

## What it does

**Save a purchase**

```bash
curl -X POST http://localhost:8080/api/v1/purchases \
  -H 'Content-Type: application/json' \
  -d '{"description":"Office supplies","transactionDate":"2025-12-31","amount":"19.995"}'
```

You get back the purchase with an id. The amount is rounded to the nearest cent, so `19.995`
is stored as `20.00`.

**Look up a purchase**

```bash
curl http://localhost:8080/api/v1/purchases/{id}
```

**Look up a purchase in another currency**

```bash
curl "http://localhost:8080/api/v1/purchases/{id}/converted?currency=Canada-Dollar"
```

This uses the most recent Treasury rate from the six months up to the purchase date. If there
is no rate in that window, you get an error. The currency name is the one the Treasury uses,
for example `Canada-Dollar`, `Mexico-Peso`, or `Euro Zone-Euro`.

## Good to know

- All the money math uses exact decimals, never floating point.
- Purchases are held in memory, so they are cleared when the app restarts.
- Saving and reading a purchase work offline. Converting needs internet access to reach the Treasury.

## Tests

```bash
./mvnw verify
```

The tests run without internet — the Treasury is stubbed out while they run.
