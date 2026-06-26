# EcoVolt Billing — Postman / Newman API Test Suite

End-to-end and negative-path API tests for the `billing-service`, runnable locally via Newman or in GitHub Actions.

---

## Directory structure

```
postman/
├── collections/
│   └── EcoVolt-Billing.postman_collection.json   # Full collection
├── environments/
│   ├── local.postman_environment.json             # Local development
│   └── ci.postman_environment.json                # CI pipeline
├── scripts/
│   └── run-newman.sh                              # POSIX runner script
└── README.md                                      # This file
```

---

## Collection coverage

| Folder | Requests |
|--------|----------|
| Setup | Health check, seed unique run variables |
| Customer APIs | POST, GET list, GET by id, PUT, GET /invoices, GET /meters, DELETE (soft-deactivate), verify INACTIVE |
| Tariff APIs | POST, GET list, GET by id, PUT deactivate |
| Meter APIs | POST, GET list, GET by id |
| Reading APIs | POST ×2, GET list |
| Invoice APIs | POST generate, GET list, GET by id, POST pay, POST cancel |
| E2E Invoice Lifecycle | 9-step flow: Customer → Tariff → Meter → Reading ×2 → Generate → Retrieve → Pay → Verify PAID |
| Negative Tests | Duplicate customer contract-gap check, 422 duplicate meter, 409/422 duplicate reading date, 422 non-monotonic reading, 422 invoice replay, 422 invalid lifecycle transitions, 400 validation errors, 404 missing entities, 400 invalid tariff slabs, 422 duplicate tariff |

### Automated test assertions per request
- HTTP status code
- Response body schema (key presence and types)
- Paginated response shape (`content`, `totalElements`, `totalPages`, `size`, `number`)
- Business rules (e.g. `unitsConsumed == 250`, `tariffType == RESIDENTIAL`, `status == PAID`)
- Error response shape (`timestamp`, `status`, `error`, `message`, `path`, `fieldErrors`)
- Variable chaining (stores `customerId`, `meterId`, `readingId1/2`, `invoiceId`, `tariffId` from responses)

---

## Prerequisites

```bash
npm install -g newman newman-reporter-htmlextra
```

---

## Importing into Postman

1. Open **Postman → File → Import**.
2. Import `postman/collections/EcoVolt-Billing.postman_collection.json`.
3. Import `postman/environments/local.postman_environment.json`.
4. Select the **EcoVolt Billing - Local** environment from the top-right dropdown.
5. Run the collection with **Run Collection** or use the **Collection Runner**.

---

## Running locally with Newman

Start `billing-service` first (default port `8080`), then:

```bash
# From project root
./postman/scripts/run-newman.sh local

# Override baseUrl explicitly
./postman/scripts/run-newman.sh local http://localhost:9090

# Or via environment variable
BILLING_BASE_URL=http://localhost:9090 ./postman/scripts/run-newman.sh local
```

Reports are written to `newman-reports/` at the project root:
- `report-<timestamp>.html` — human-readable HTML report
- `junit-<timestamp>.xml` — JUnit XML for CI integration

---

## Running in CI/CD

The GitHub Actions workflow at `.github/workflows/postman-newman.yml` performs the full pipeline automatically on every push and pull request to `main`.

To trigger manually from the Actions tab, use **workflow_dispatch**.

**What the workflow does:**
1. Checks out the repository.
2. Sets up Java 25 (Temurin) and builds the service with Gradle.
3. Starts `billing-service` in the background.
4. Polls `GET /actuator/health` until the service is UP (60 s timeout).
5. Installs Node.js and Newman.
6. Runs the full collection against the CI environment.
7. Uploads the HTML and JUnit reports as workflow artifacts.
8. Publishes the JUnit report via the test-reporter action (visible under **Checks**).

---

## Environment configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `baseUrl` | `http://localhost:8080` | Service base URL |
| `customerId` | *(auto-set)* | Set by Create Customer pre-request script |
| `meterId` | *(auto-set)* | Set by Register Meter test script |
| `readingId1` | *(auto-set)* | Set by Add Reading 1 test script |
| `readingId2` | *(auto-set)* | Set by Add Reading 2 test script |
| `invoiceId` | *(auto-set)* | Set by Generate Invoice test script |
| `tariffId` | *(auto-set)* | Set by Create Tariff Plan test script |

**No secrets are stored in environment files.** All values are either empty defaults or auto-populated by collection scripts at runtime.

To point at a non-default URL, either:
- Edit `baseUrl` in the environment file before import, or
- Pass `--env-var "baseUrl=<url>"` to Newman directly, or
- Set `BILLING_BASE_URL` environment variable before running `run-newman.sh`.

---

## Notes

- **Unique data per run**: The Setup folder's pre-request script generates a timestamp-based email and meter number so concurrent runs don't conflict.
- **No teardown DELETEs**: Meter, reading, and invoice records are not deleted after tests. Customer soft-delete (`DELETE /api/customers/{id}`) transitions status to `INACTIVE` and is included as a functional test, not a teardown step.
- **E2E folder is self-contained**: It creates its own customer, meter, readings, and invoice using `e2e_*` prefixed variables, and verifies the seeded active RESIDENTIAL tariff used for invoice calculation.
- **Duplicate customer gap**: The current implementation does not enforce unique customer email. The collection includes this as a contract-gap check instead of asserting a failing 409 that the API does not currently return.
