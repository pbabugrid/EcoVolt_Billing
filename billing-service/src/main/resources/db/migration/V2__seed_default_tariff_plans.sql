INSERT INTO tariff_plans (tariff_type, version, name, effective_from, effective_to, active)
VALUES
    ('RESIDENTIAL', 1, 'Residential v1', DATE '2024-01-01', NULL, TRUE),
    ('COMMERCIAL', 1, 'Commercial v1', DATE '2024-01-01', NULL, TRUE),
    ('INDUSTRIAL', 1, 'Industrial v1', DATE '2024-01-01', NULL, TRUE);

INSERT INTO tariff_slabs (sort_order, from_units, to_units, rate_per_unit, tariff_plan_id)
VALUES
    (1, 0.00, 100.00, 5.00, (SELECT id FROM tariff_plans WHERE tariff_type = 'RESIDENTIAL' AND version = 1)),
    (2, 100.00, 300.00, 7.00, (SELECT id FROM tariff_plans WHERE tariff_type = 'RESIDENTIAL' AND version = 1)),
    (3, 300.00, NULL, 10.00, (SELECT id FROM tariff_plans WHERE tariff_type = 'RESIDENTIAL' AND version = 1)),
    (1, 0.00, 100.00, 8.00, (SELECT id FROM tariff_plans WHERE tariff_type = 'COMMERCIAL' AND version = 1)),
    (2, 100.00, 300.00, 10.00, (SELECT id FROM tariff_plans WHERE tariff_type = 'COMMERCIAL' AND version = 1)),
    (3, 300.00, NULL, 12.00, (SELECT id FROM tariff_plans WHERE tariff_type = 'COMMERCIAL' AND version = 1)),
    (1, 0.00, 100.00, 10.00, (SELECT id FROM tariff_plans WHERE tariff_type = 'INDUSTRIAL' AND version = 1)),
    (2, 100.00, 300.00, 12.00, (SELECT id FROM tariff_plans WHERE tariff_type = 'INDUSTRIAL' AND version = 1)),
    (3, 300.00, NULL, 15.00, (SELECT id FROM tariff_plans WHERE tariff_type = 'INDUSTRIAL' AND version = 1));
