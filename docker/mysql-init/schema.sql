CREATE TABLE customers
(
    customer_id INT AUTO_INCREMENT  PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(100) NOT NULL
);

-- 2. Meters — each customer can have 1 or 2 meters
CREATE TABLE meters
(
    meter_id     INT AUTO_INCREMENT      PRIMARY KEY,
    customer_id  INT         NOT NULL,
    meter_number VARCHAR(50) NOT NULL UNIQUE,
    FOREIGN KEY (customer_id) REFERENCES customers (customer_id)

);

-- 3. Unit Prices — one row per interval type, loaded at runtime
CREATE TABLE unit_prices
(
    price_id       int AUTO_INCREMENT        PRIMARY KEY,
    interval_type  VARCHAR(20)   NOT NULL UNIQUE, -- OFF_PEAK | STANDARD | PEAK
    price_per_unit NUMERIC(8, 4) NOT NULL
);

-- 4. Bills — one row per customer per batch run, FK to customers
CREATE TABLE bills
(
    bill_id        int AUTO_INCREMENT         PRIMARY KEY,
    customer_id    INT            NOT NULL,
    billing_start  DATE           NOT NULL,
    billing_end    DATE           NOT NULL,
    off_peak_units NUMERIC(10, 2) NOT NULL,
    standard_units NUMERIC(10, 2) NOT NULL,
    peak_units     NUMERIC(10, 2) NOT NULL,
    total_units    NUMERIC(10, 2) NOT NULL,
    total_amount   NUMERIC(10, 2) NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
);

-- Seed unit prices (safe to re-run)
INSERT INTO unit_prices (interval_type, price_per_unit)
VALUES ('OFF_PEAK', 5.00),
       ('STANDARD', 4.00),
       ('PEAK', 7.00)

