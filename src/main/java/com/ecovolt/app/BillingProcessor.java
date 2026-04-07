package com.ecovolt.app;

import com.ecovolt.model.CsvRow;
import com.ecovolt.model.CustomerBill;
import com.ecovolt.strategy.PricingStrategyFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Core billing logic — applies the Strategy Pattern to compute costs.
 * <p>
 * This class is pure: it takes rows and strategies as input and returns bills.
 * No DB calls, no file I/O — the easiest class to unit test.
 * <p>
 * Strategy Pattern in action:
 * Each interval column maps to its own PricingStrategy.
 * The processor delegates cost calculation to each strategy — it doesn't know the rate.
 */
public class BillingProcessor {

    private final PricingStrategyFactory strategies;

    public BillingProcessor(PricingStrategyFactory strategies) {
        this.strategies = strategies;
    }

    /**
     * Processes all CSV rows and accumulates one CustomerBill per customer.
     * Multiple meters per customer are handled transparently — all rows for the
     * same customer_id are summed into a single bill.
     */
    public Collection<CustomerBill> process(List<CsvRow> rows) {
        Map<Integer, CustomerBill> bills = new LinkedHashMap<>();

        for (CsvRow row : rows) {
            // Strategy Pattern: each strategy calculates cost for its own interval
            double cost = strategies.offPeak().calculateCost(row.getOffPeakUnits())
                    + strategies.standard().calculateCost(row.getStandardUnits())
                    + strategies.peak().calculateCost(row.getPeakUnits());

            bills.computeIfAbsent(row.getCustomerId(), CustomerBill::new)
                    .add(row.getReadingDate(),
                            row.getOffPeakUnits(),
                            row.getStandardUnits(),
                            row.getPeakUnits(),
                            cost);
        }

        return bills.values();
    }
}
