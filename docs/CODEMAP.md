# CODEMAP

*   `src/main/java/com/ecovolt/`
    *   `app/`: Main application entry points and core processing logic (e.g., `MainApplication.java`, `BillingProcessor.java`, `CsvReader.java`).
    *   `config/`: Configuration classes (e.g., `DatabaseConfig.java`).
    *   `dao/`: Data Access Objects for database interactions (e.g., `CustomerDAO.java`, `MeterDAO.java`, `BillingDAO.java`, `UnitPriceDAO.java`).
    *   `model/`: Domain models and entities (e.g., `Customer.java`, `Meter.java`, `CustomerBill.java`, `UnitPrice.java`).
    *   `strategy/`: Pricing strategy implementation (e.g., `PricingStrategy.java`, `StandardPricingStrategy.java`, `PeakPricingStrategy.java`).
*   `src/main/resources/`: Configuration files and static assets (e.g., `meter_readings.csv`).
*   `docker/`: Docker related configurations including `mysql-init/schema.sql` and `docker-compose.yml`.
