-- =============================================================================
-- V4__seed_reference_and_demo_data.sql
-- EcoVolt Billing — Reference & Demo Seed Data
--
-- Inserts fictional demo customers, meters, meter readings, and invoices that
-- span all three tariff types (RESIDENTIAL, COMMERCIAL, INDUSTRIAL). Active
-- current tariff plans are seeded by V2 and reused here; V4 does not add more
-- tariff plan rows.
--
-- FK references use subqueries on stable natural keys (customer_number,
-- meter_number) so no hard-coded generated IDs are required.
--
-- Seeded invoices use historical reading pairs only. The latest reading pairs
-- remain uninvoiced so InvoiceGenerationService.generateForCustomer() can still
-- produce invoices for the demo customers.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. CUSTOMERS  (one per demo scenario; .test domain; no real PII)
-- ---------------------------------------------------------------------------
INSERT INTO customers (customer_number, name, email, phone, address, status, created_at, updated_at)
VALUES
    -- Residential customers (3)
    ('CUST-R001', 'Alice Greenfield',   'alice.greenfield@ecovolt.test',    '555-0101', '12 Maple Lane, Springfield, SP 10101',    'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CUST-R002', 'Bob Harrington',     'bob.harrington@ecovolt.test',      '555-0102', '47 Oak Avenue, Riverdale, RD 20202',      'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CUST-R003', 'Clara Nguyen',       'clara.nguyen@ecovolt.test',        '555-0103', '9 Birch Street, Lakewood, LW 30303',      'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Commercial customers (2)
    ('CUST-C001', 'Bright Bean Cafe Ltd',      'billing@brightbeancafe.test',     '555-0201', '88 Commerce Drive, Midtown, MT 40404',    'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CUST-C002', 'Maple Office Park Inc',     'accounts@mapleofficepark.test',   '555-0202', '210 Business Blvd, Uptown, UT 50505',     'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Industrial customers (2)
    ('CUST-I001', 'Ironbridge Manufacturing Co',   'billing@ironbridgemfg.test',  '555-0301', '1 Industrial Way, Steelton, ST 60606',    'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CUST-I002', 'Northgate Cold Storage LLC',   'ops@northgatecold.test',       '555-0302', '77 Freight Road, Coldport, CP 70707',     'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ---------------------------------------------------------------------------
-- 2. METERS  (one per customer; tariff_type matches the V2 active plan)
-- ---------------------------------------------------------------------------
INSERT INTO meters (meter_number, installation_date, status, tariff_type, customer_id, created_at, updated_at)
VALUES
    -- Residential meters
    ('MTR-RES-001', DATE '2024-01-15', 'ACTIVE', 'RESIDENTIAL', (SELECT id FROM customers WHERE customer_number = 'CUST-R001'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MTR-RES-002', DATE '2024-02-01', 'ACTIVE', 'RESIDENTIAL', (SELECT id FROM customers WHERE customer_number = 'CUST-R002'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MTR-RES-003', DATE '2024-01-20', 'ACTIVE', 'RESIDENTIAL', (SELECT id FROM customers WHERE customer_number = 'CUST-R003'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Commercial meters
    ('MTR-COM-001', DATE '2024-01-10', 'ACTIVE', 'COMMERCIAL', (SELECT id FROM customers WHERE customer_number = 'CUST-C001'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MTR-COM-002', DATE '2024-03-01', 'ACTIVE', 'COMMERCIAL', (SELECT id FROM customers WHERE customer_number = 'CUST-C002'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Industrial meters
    ('MTR-IND-001', DATE '2024-01-05', 'ACTIVE', 'INDUSTRIAL', (SELECT id FROM customers WHERE customer_number = 'CUST-I001'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MTR-IND-002', DATE '2024-02-15', 'ACTIVE', 'INDUSTRIAL', (SELECT id FROM customers WHERE customer_number = 'CUST-I002'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ---------------------------------------------------------------------------
-- 3. METER READINGS  (≥ 2 per meter; strictly increasing; unique meter+date)
--
-- Consumption notes (for spot-checking against V2 tariff slabs):
--
-- RESIDENTIAL slabs: 0-100 @ 5.00 | 100-300 @ 7.00 | 300+ @ 10.00
--   MTR-RES-001: 245.00 - 100.00 = 145 units  → 100*5 + 45*7  = 815.00
--   MTR-RES-002: 295.00 - 180.00 = 115 units  → 100*5 + 15*7  = 605.00
--   MTR-RES-003: 720.00 - 510.00 = 210 units  → 100*5 + 110*7 = 1270.00
--
-- COMMERCIAL slabs: 0-100 @ 8.00 | 100-300 @ 10.00 | 300+ @ 12.00
--   MTR-COM-001: 480.00 - 200.00 = 280 units  → 100*8 + 180*10 = 2600.00
--   MTR-COM-002: 760.00 - 420.00 = 340 units  → 100*8 + 200*10 + 40*12 = 3280.00
--
-- INDUSTRIAL slabs: 0-100 @ 10.00 | 100-300 @ 12.00 | 300+ @ 15.00
--   MTR-IND-001: 1930.00 - 1450.00 = 480 units → 100*10 + 200*12 + 180*15 = 6100.00
--   MTR-IND-002:  980.00 -  500.00 = 480 units → 100*10 + 200*12 + 180*15 = 6100.00
-- ---------------------------------------------------------------------------
INSERT INTO meter_readings (reading_date, reading_value, meter_id, created_at, updated_at)
VALUES
    -- MTR-RES-001 (Alice Greenfield) — 2 readings
    (DATE '2024-02-01', 100.00, (SELECT id FROM meters WHERE meter_number = 'MTR-RES-001'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (DATE '2024-03-01', 245.00, (SELECT id FROM meters WHERE meter_number = 'MTR-RES-001'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- MTR-RES-002 (Bob Harrington) — 3 readings (additional historical reading)
    (DATE '2024-02-01',  50.00, (SELECT id FROM meters WHERE meter_number = 'MTR-RES-002'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (DATE '2024-03-01', 180.00, (SELECT id FROM meters WHERE meter_number = 'MTR-RES-002'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (DATE '2024-04-01', 295.00, (SELECT id FROM meters WHERE meter_number = 'MTR-RES-002'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- MTR-RES-003 (Clara Nguyen) — 3 readings (crosses upper residential slab)
    (DATE '2024-02-15', 320.00, (SELECT id FROM meters WHERE meter_number = 'MTR-RES-003'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (DATE '2024-03-15', 510.00, (SELECT id FROM meters WHERE meter_number = 'MTR-RES-003'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (DATE '2024-04-15', 720.00, (SELECT id FROM meters WHERE meter_number = 'MTR-RES-003'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- MTR-COM-001 (Bright Bean Cafe Ltd) — 2 readings (crosses commercial slabs)
    (DATE '2024-02-01', 200.00, (SELECT id FROM meters WHERE meter_number = 'MTR-COM-001'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (DATE '2024-03-01', 480.00, (SELECT id FROM meters WHERE meter_number = 'MTR-COM-001'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- MTR-COM-002 (Maple Office Park Inc) — 3 readings (crosses all 3 commercial slabs)
    (DATE '2024-04-01', 100.00, (SELECT id FROM meters WHERE meter_number = 'MTR-COM-002'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (DATE '2024-05-01', 420.00, (SELECT id FROM meters WHERE meter_number = 'MTR-COM-002'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (DATE '2024-06-01', 760.00, (SELECT id FROM meters WHERE meter_number = 'MTR-COM-002'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- MTR-IND-001 (Ironbridge Manufacturing Co) — 3 readings (high-volume industrial)
    (DATE '2024-02-01', 1000.00, (SELECT id FROM meters WHERE meter_number = 'MTR-IND-001'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (DATE '2024-03-01', 1450.00, (SELECT id FROM meters WHERE meter_number = 'MTR-IND-001'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (DATE '2024-04-01', 1930.00, (SELECT id FROM meters WHERE meter_number = 'MTR-IND-001'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- MTR-IND-002 (Northgate Cold Storage LLC) — 2 readings (crosses all 3 industrial slabs)
    (DATE '2024-03-15',  500.00, (SELECT id FROM meters WHERE meter_number = 'MTR-IND-002'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (DATE '2024-04-15',  980.00, (SELECT id FROM meters WHERE meter_number = 'MTR-IND-002'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ---------------------------------------------------------------------------
-- 4. INVOICES  (historical pairs only; latest pairs remain available)
--
-- These rows make GET /api/invoices useful immediately while preserving invoice
-- generation demos for every seeded meter.
-- ---------------------------------------------------------------------------
INSERT INTO invoices (
    invoice_number,
    previous_reading,
    current_reading,
    units_consumed,
    amount,
    tariff_type,
    tariff_version,
    generated_date,
    status,
    customer_id,
    previous_reading_id,
    current_reading_id,
    tariff_plan_id,
    created_at,
    updated_at
)
VALUES
    (
        'INV-DEMO-R002-202403',
        50.00,
        180.00,
        130.00,
        710.00,
        'RESIDENTIAL',
        1,
        DATE '2024-03-02',
        'GENERATED',
        (SELECT id FROM customers WHERE customer_number = 'CUST-R002'),
        (SELECT mr.id FROM meter_readings mr JOIN meters m ON mr.meter_id = m.id WHERE m.meter_number = 'MTR-RES-002' AND mr.reading_date = DATE '2024-02-01'),
        (SELECT mr.id FROM meter_readings mr JOIN meters m ON mr.meter_id = m.id WHERE m.meter_number = 'MTR-RES-002' AND mr.reading_date = DATE '2024-03-01'),
        (SELECT id FROM tariff_plans WHERE tariff_type = 'RESIDENTIAL' AND version = 1),
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'INV-DEMO-R003-202403',
        320.00,
        510.00,
        190.00,
        1130.00,
        'RESIDENTIAL',
        1,
        DATE '2024-03-16',
        'GENERATED',
        (SELECT id FROM customers WHERE customer_number = 'CUST-R003'),
        (SELECT mr.id FROM meter_readings mr JOIN meters m ON mr.meter_id = m.id WHERE m.meter_number = 'MTR-RES-003' AND mr.reading_date = DATE '2024-02-15'),
        (SELECT mr.id FROM meter_readings mr JOIN meters m ON mr.meter_id = m.id WHERE m.meter_number = 'MTR-RES-003' AND mr.reading_date = DATE '2024-03-15'),
        (SELECT id FROM tariff_plans WHERE tariff_type = 'RESIDENTIAL' AND version = 1),
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'INV-DEMO-C002-202405',
        100.00,
        420.00,
        320.00,
        3040.00,
        'COMMERCIAL',
        1,
        DATE '2024-05-02',
        'GENERATED',
        (SELECT id FROM customers WHERE customer_number = 'CUST-C002'),
        (SELECT mr.id FROM meter_readings mr JOIN meters m ON mr.meter_id = m.id WHERE m.meter_number = 'MTR-COM-002' AND mr.reading_date = DATE '2024-04-01'),
        (SELECT mr.id FROM meter_readings mr JOIN meters m ON mr.meter_id = m.id WHERE m.meter_number = 'MTR-COM-002' AND mr.reading_date = DATE '2024-05-01'),
        (SELECT id FROM tariff_plans WHERE tariff_type = 'COMMERCIAL' AND version = 1),
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'INV-DEMO-I001-202403',
        1000.00,
        1450.00,
        450.00,
        5650.00,
        'INDUSTRIAL',
        1,
        DATE '2024-03-02',
        'GENERATED',
        (SELECT id FROM customers WHERE customer_number = 'CUST-I001'),
        (SELECT mr.id FROM meter_readings mr JOIN meters m ON mr.meter_id = m.id WHERE m.meter_number = 'MTR-IND-001' AND mr.reading_date = DATE '2024-02-01'),
        (SELECT mr.id FROM meter_readings mr JOIN meters m ON mr.meter_id = m.id WHERE m.meter_number = 'MTR-IND-001' AND mr.reading_date = DATE '2024-03-01'),
        (SELECT id FROM tariff_plans WHERE tariff_type = 'INDUSTRIAL' AND version = 1),
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );
