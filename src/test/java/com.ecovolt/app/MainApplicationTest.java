package com.ecovolt.app;

import com.ecovolt.dao.BillingDAO;
import com.ecovolt.dao.CustomerDAO;
import com.ecovolt.dao.MeterDAO;
import com.ecovolt.dao.UnitPriceDAO;
import com.ecovolt.model.UnitPrice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests MainApplication.run() — the orchestrator — by mocking all four DAOs
 * so no real DB or CSV file is needed.
 */
@ExtendWith(MockitoExtension.class)
class MainApplicationTest {

    @Mock
    Connection connection;
    @Mock
    PreparedStatement ps;
    @Mock
    ResultSet rs;

    private static final String HEADER =
            "customer_id,meter_id,reading_date,off_peak_units,standard_units,peak_units\n";

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Write a small CSV to a temp file and return its path string.
     */
    private static String writeCsv(Path dir, String rows) throws Exception {
        Path f = dir.resolve("test.csv");
        Files.writeString(f, HEADER + rows);
        return f.toString();
    }

    /**
     * Builds a stubbed UnitPriceDAO that returns the three default rates.
     */
    private static List<UnitPrice> defaultPrices() {
        return List.of(
                new UnitPrice(1, "OFF_PEAK", 5.0),
                new UnitPrice(2, "STANDARD", 4.0),
                new UnitPrice(3, "PEAK", 7.0)
        );
    }

    // ── run() happy path ──────────────────────────────────────────────────────

    @Test
    void run_twoCustomers_upsertsBothBills(@TempDir Path tmp) throws Exception {
        String csv = writeCsv(tmp,
                "1,1,2026-03-01,1.0,1.0,1.0\n" +
                        "2,2,2026-03-01,2.0,2.0,2.0\n");

        // CustomerDAO and MeterDAO: findById returns empty → triggers insert
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false); // not found → upsert inserts

        try (MockedConstruction<UnitPriceDAO> unitPriceDAOMock =
                     mockConstruction(UnitPriceDAO.class,
                             (mock, ctx) -> when(mock.findAll(connection)).thenReturn(defaultPrices()));
             MockedConstruction<BillingDAO> billingDAOMock =
                     mockConstruction(BillingDAO.class,
                             (mock, ctx) -> when(mock.batchInsertBills(eq(connection), any()))
                                     .thenReturn(2))) {

            assertDoesNotThrow(() -> MainApplication.run(connection, csv));

            // BillingDAO.batchUpsertBills must be called exactly once
            BillingDAO billingDAO = billingDAOMock.constructed().get(0);
            verify(billingDAO, times(1)).batchInsertBills(eq(connection), argThat(
                    bills -> bills.size() == 2
            ));
        }
    }

    @Test
    void run_singleRow_upsertsCalled(@TempDir Path tmp) throws Exception {
        String csv = writeCsv(tmp, "5,5,2026-03-01,1.0,2.0,3.0\n");

        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        try (MockedConstruction<UnitPriceDAO> ignored1 =
                     mockConstruction(UnitPriceDAO.class,
                             (mock, ctx) -> when(mock.findAll(connection)).thenReturn(defaultPrices()));
             MockedConstruction<BillingDAO> billingMock =
                     mockConstruction(BillingDAO.class,
                             (mock, ctx) -> when(mock.batchInsertBills(eq(connection), any()))
                                     .thenReturn(1))) {

            MainApplication.run(connection, csv);

            verify(billingMock.constructed().get(0))
                    .batchInsertBills(eq(connection), argThat(bills -> bills.size() == 1));
        }
    }

    @Test
    void run_twoMeters_sameCustomer_producesOneBill(@TempDir Path tmp) throws Exception {
        // meter_id=1 and meter_id=2 both belong to customer_id=1
        String csv = writeCsv(tmp,
                "1,1,2026-03-01,1.0,0.0,0.0\n" +
                        "1,2,2026-03-01,0.0,1.0,0.0\n");

        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        try (MockedConstruction<UnitPriceDAO> ignored =
                     mockConstruction(UnitPriceDAO.class,
                             (mock, ctx) -> when(mock.findAll(connection)).thenReturn(defaultPrices()));
             MockedConstruction<BillingDAO> billingMock =
                     mockConstruction(BillingDAO.class,
                             (mock, ctx) -> when(mock.batchInsertBills(eq(connection), any()))
                                     .thenReturn(1))) {

            MainApplication.run(connection, csv);

            verify(billingMock.constructed().get(0))
                    .batchInsertBills(eq(connection), argThat(bills -> bills.size() == 1));
        }
    }

    @Test
    void run_unitPricesLoadedFromDAO(@TempDir Path tmp) throws Exception {
        String csv = writeCsv(tmp, "1,1,2026-03-01,1.0,1.0,1.0\n");

        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        try (MockedConstruction<UnitPriceDAO> unitMock =
                     mockConstruction(UnitPriceDAO.class,
                             (mock, ctx) -> when(mock.findAll(connection)).thenReturn(defaultPrices()));
             MockedConstruction<BillingDAO> ignored =
                     mockConstruction(BillingDAO.class,
                             (mock, ctx) -> when(mock.batchInsertBills(eq(connection), any()))
                                     .thenReturn(1))) {

            MainApplication.run(connection, csv);

            // UnitPriceDAO.findAll must have been called with the connection
            verify(unitMock.constructed().get(0)).findAll(connection);
        }
    }
}
