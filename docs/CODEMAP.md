# Code Map

- `src/main/java/com/ecovolt/app/`: Core application entry and processing
  - `MainApplication.java`: Application entry point.
  - `BillingProcessor.java`: Controls billing orchestration.
  - `CsvReader.java`: CSV ingestion helper.
- `src/main/java/com/ecovolt/config/`: Configuration setup
  - `DatabaseConfig.java`: SQL persistence configuration.
- `src/main/java/com/ecovolt/dao/`: Data Access layer
  - `BillingDAO.java`, `CustomerDAO.java`, `MeterDAO.java`, `UnitPriceDAO.java`: SQL statement management classes.
- `src/main/java/com/ecovolt/model/`: Entity structures
  - `CsvRow.java`, `Customer.java`, `Meter.java`, `UnitPrice.java`, `CustomerBill.java`: Core entities.
- `src/main/java/com/ecovolt/strategy/`: Price strategy definitions
  - `PricingStrategy.java`: Strategy base interface.
  - `PricingStrategyFactory.java`: Instantiates proper pricing implementations.
  - `PeakPricingStrategy.java`, `OffPeakPricingStrategy.java`, `StandardPricingStrategy.java`, `WeekendPricingStrategy.java`: Concrete pattern classes.
- `src/main/resources/meter_readings.csv`: Sample ingestion data
- `docker/`: Infrastructure configurations and database initialization
