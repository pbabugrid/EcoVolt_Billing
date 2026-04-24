# ARCHITECTURE

*   **Database Schema:** Managed by `docker/mysql-init/schema.sql` mapped via JDBC logic.
*   **Pricing:** `PricingStrategy` interface implemented by `StandardPricingStrategy`, `OffPeakPricingStrategy`, and `PeakPricingStrategy`.
*   **Data Access:** DAO pattern (`BillingDAO`, `CustomerDAO`, etc.).
*   **Batching Pipeline:** `CsvReader` -> `BillingProcessor` -> DAOs.
