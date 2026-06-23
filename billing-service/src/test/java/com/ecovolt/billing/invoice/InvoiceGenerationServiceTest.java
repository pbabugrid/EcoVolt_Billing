package com.ecovolt.billing.invoice;

import com.ecovolt.billing.customer.Customer;
import com.ecovolt.billing.customer.CustomerService;
import com.ecovolt.billing.exception.BillingException;
import com.ecovolt.billing.invoice.dto.InvoiceResponse;
import com.ecovolt.billing.meter.Meter;
import com.ecovolt.billing.meter.MeterRepository;
import com.ecovolt.billing.reading.MeterReading;
import com.ecovolt.billing.reading.MeterReadingRepository;
import com.ecovolt.billing.tariff.TariffCalculation;
import com.ecovolt.billing.tariff.TariffPlan;
import com.ecovolt.billing.tariff.TariffService;
import com.ecovolt.billing.tariff.TariffType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceGenerationServiceTest {

    @Mock MeterReadingRepository meterReadingRepository;
    @Mock MeterRepository meterRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock CustomerService customerService;
    @Mock TariffService tariffService;

    @InjectMocks InvoiceGenerationService service;

    private Customer customer;
    private Meter meter1;
    private Meter meter2;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().build();
        setId(customer, 1L);

        meter1 = Meter.builder().meterNumber("MTR-001").customer(customer).build();
        setId(meter1, 10L);

        meter2 = Meter.builder().meterNumber("MTR-002").customer(customer).build();
        setId(meter2, 20L);
    }

    // ---------- happy paths ----------

    @Test
    @DisplayName("Single meter with two readings generates one invoice")
    void singleMeter_twoReadings_createsOneInvoice() {
        MeterReading prev = reading(1L, meter1, LocalDate.of(2024, 1, 1), new BigDecimal("100.00"));
        MeterReading curr = reading(2L, meter1, LocalDate.of(2024, 2, 1), new BigDecimal("150.00"));

        when(customerService.getCustomerOrThrow(1L)).thenReturn(customer);
        when(meterRepository.findByCustomer_IdOrderByIdAsc(1L)).thenReturn(List.of(meter1));
        when(meterReadingRepository.findTop2ByMeter_IdOrderByReadingDateDescIdDesc(10L))
                .thenReturn(List.of(curr, prev));
        when(invoiceRepository.existsByCustomer_IdAndPreviousReadingRecord_IdAndCurrentReadingRecord_Id(
                anyLong(), anyLong(), anyLong())).thenReturn(false);
        when(invoiceRepository.existsByInvoiceNumber(anyString())).thenReturn(false);
        Invoice saved = savedInvoice(customer, prev, curr, new BigDecimal("50.00"), new BigDecimal("250.00"));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(saved);
        when(tariffService.calculateAmount(any(TariffType.class), any(BigDecimal.class), any(LocalDate.class)))
                .thenReturn(tariffCalculation(new BigDecimal("250.00")));

        List<InvoiceResponse> result = service.generateForCustomer(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).unitsConsumed()).isEqualByComparingTo("50.00");
        assertThat(result.get(0).amount()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("Multi-meter customer generates one invoice per eligible meter")
    void multiMeter_bothEligible_createsTwoInvoices() {
        MeterReading m1prev = reading(1L, meter1, LocalDate.of(2024, 1, 1), new BigDecimal("100.00"));
        MeterReading m1curr = reading(2L, meter1, LocalDate.of(2024, 2, 1), new BigDecimal("160.00"));
        MeterReading m2prev = reading(3L, meter2, LocalDate.of(2024, 1, 1), new BigDecimal("200.00"));
        MeterReading m2curr = reading(4L, meter2, LocalDate.of(2024, 2, 1), new BigDecimal("280.00"));

        when(customerService.getCustomerOrThrow(1L)).thenReturn(customer);
        when(meterRepository.findByCustomer_IdOrderByIdAsc(1L)).thenReturn(List.of(meter1, meter2));
        when(meterReadingRepository.findTop2ByMeter_IdOrderByReadingDateDescIdDesc(10L))
                .thenReturn(List.of(m1curr, m1prev));
        when(meterReadingRepository.findTop2ByMeter_IdOrderByReadingDateDescIdDesc(20L))
                .thenReturn(List.of(m2curr, m2prev));
        when(invoiceRepository.existsByCustomer_IdAndPreviousReadingRecord_IdAndCurrentReadingRecord_Id(
                anyLong(), anyLong(), anyLong())).thenReturn(false);
        when(invoiceRepository.existsByInvoiceNumber(anyString())).thenReturn(false);
        when(tariffService.calculateAmount(any(TariffType.class), any(BigDecimal.class), any(LocalDate.class)))
                .thenReturn(tariffCalculation(new BigDecimal("300.00")));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            setId(i, (long) (Math.random() * 1000 + 1));
            return i;
        });

        List<InvoiceResponse> result = service.generateForCustomer(1L);

        assertThat(result).hasSize(2);
        verify(invoiceRepository, times(2)).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Readings for each meter come from the same meter only")
    void eachInvoiceUsesReadingsFromSameMeter() {
        MeterReading m1prev = reading(1L, meter1, LocalDate.of(2024, 1, 1), new BigDecimal("100.00"));
        MeterReading m1curr = reading(2L, meter1, LocalDate.of(2024, 2, 1), new BigDecimal("150.00"));

        when(customerService.getCustomerOrThrow(1L)).thenReturn(customer);
        when(meterRepository.findByCustomer_IdOrderByIdAsc(1L)).thenReturn(List.of(meter1));
        when(meterReadingRepository.findTop2ByMeter_IdOrderByReadingDateDescIdDesc(10L))
                .thenReturn(List.of(m1curr, m1prev));
        when(invoiceRepository.existsByCustomer_IdAndPreviousReadingRecord_IdAndCurrentReadingRecord_Id(
                anyLong(), anyLong(), anyLong())).thenReturn(false);
        when(invoiceRepository.existsByInvoiceNumber(anyString())).thenReturn(false);
        when(tariffService.calculateAmount(any(TariffType.class), any(BigDecimal.class), any(LocalDate.class)))
                .thenReturn(tariffCalculation(new BigDecimal("250.00")));
        Invoice saved = savedInvoice(customer, m1prev, m1curr, new BigDecimal("50.00"), new BigDecimal("250.00"));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(saved);

        service.generateForCustomer(1L);

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        Invoice persisted = captor.getValue();
        assertThat(persisted.getPreviousReadingRecord().getMeter().getId())
                .isEqualTo(persisted.getCurrentReadingRecord().getMeter().getId());
    }

    @Test
    @DisplayName("Latest-two selector uses reading date and id descending order")
    void latestTwoSelectorUsesDateAndIdDescendingOrder() {
        MeterReading prev = reading(1L, meter1, LocalDate.of(2024, 1, 1), new BigDecimal("100.00"));
        MeterReading curr = reading(2L, meter1, LocalDate.of(2024, 2, 1), new BigDecimal("150.00"));

        when(customerService.getCustomerOrThrow(1L)).thenReturn(customer);
        when(meterRepository.findByCustomer_IdOrderByIdAsc(1L)).thenReturn(List.of(meter1));
        when(meterReadingRepository.findTop2ByMeter_IdOrderByReadingDateDescIdDesc(10L))
                .thenReturn(List.of(curr, prev));
        when(invoiceRepository.existsByCustomer_IdAndPreviousReadingRecord_IdAndCurrentReadingRecord_Id(
                anyLong(), anyLong(), anyLong())).thenReturn(false);
        when(invoiceRepository.existsByInvoiceNumber(anyString())).thenReturn(false);
        when(tariffService.calculateAmount(any(TariffType.class), any(BigDecimal.class), any(LocalDate.class)))
                .thenReturn(tariffCalculation(new BigDecimal("250.00")));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(
                savedInvoice(customer, prev, curr, new BigDecimal("50.00"), new BigDecimal("250.00")));

        service.generateForCustomer(1L);

        verify(meterReadingRepository).findTop2ByMeter_IdOrderByReadingDateDescIdDesc(10L);
    }

    // ---------- FR6: no meter has two readings ----------

    @Test
    @DisplayName("No meter with two readings throws 422")
    void noMeterWithTwoReadings_throws422() {
        when(customerService.getCustomerOrThrow(1L)).thenReturn(customer);
        when(meterRepository.findByCustomer_IdOrderByIdAsc(1L)).thenReturn(List.of(meter1));
        when(meterReadingRepository.findTop2ByMeter_IdOrderByReadingDateDescIdDesc(10L))
                .thenReturn(List.of()); // none

        assertThatThrownBy(() -> service.generateForCustomer(1L))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("At least two meter readings");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Meter with only one reading is not eligible")
    void meterWithOneReading_notEligible_throws422() {
        MeterReading only = reading(1L, meter1, LocalDate.of(2024, 1, 1), new BigDecimal("100.00"));

        when(customerService.getCustomerOrThrow(1L)).thenReturn(customer);
        when(meterRepository.findByCustomer_IdOrderByIdAsc(1L)).thenReturn(List.of(meter1));
        when(meterReadingRepository.findTop2ByMeter_IdOrderByReadingDateDescIdDesc(10L))
                .thenReturn(List.of(only));

        assertThatThrownBy(() -> service.generateForCustomer(1L))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("At least two meter readings");
    }

    @Test
    @DisplayName("Customer with no meters throws 422")
    void noMeters_throws422() {
        when(customerService.getCustomerOrThrow(1L)).thenReturn(customer);
        when(meterRepository.findByCustomer_IdOrderByIdAsc(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.generateForCustomer(1L))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("At least two meter readings");
    }

    // ---------- FR5: all eligible pairs already invoiced ----------

    @Test
    @DisplayName("All eligible pairs are duplicates throws 422")
    void allPairsDuplicate_throws422() {
        MeterReading prev = reading(1L, meter1, LocalDate.of(2024, 1, 1), new BigDecimal("100.00"));
        MeterReading curr = reading(2L, meter1, LocalDate.of(2024, 2, 1), new BigDecimal("150.00"));

        when(customerService.getCustomerOrThrow(1L)).thenReturn(customer);
        when(meterRepository.findByCustomer_IdOrderByIdAsc(1L)).thenReturn(List.of(meter1));
        when(meterReadingRepository.findTop2ByMeter_IdOrderByReadingDateDescIdDesc(10L))
                .thenReturn(List.of(curr, prev));
        when(invoiceRepository.existsByCustomer_IdAndPreviousReadingRecord_IdAndCurrentReadingRecord_Id(
                anyLong(), anyLong(), anyLong())).thenReturn(true); // already invoiced

        assertThatThrownBy(() -> service.generateForCustomer(1L))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("already been invoiced");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Multi-meter: one meter is duplicate, other is new — only new is created")
    void multiMeter_oneNew_oneDuplicate_createsOne() {
        MeterReading m1prev = reading(1L, meter1, LocalDate.of(2024, 1, 1), new BigDecimal("100.00"));
        MeterReading m1curr = reading(2L, meter1, LocalDate.of(2024, 2, 1), new BigDecimal("150.00"));
        MeterReading m2prev = reading(3L, meter2, LocalDate.of(2024, 1, 1), new BigDecimal("200.00"));
        MeterReading m2curr = reading(4L, meter2, LocalDate.of(2024, 2, 1), new BigDecimal("280.00"));

        when(customerService.getCustomerOrThrow(1L)).thenReturn(customer);
        when(meterRepository.findByCustomer_IdOrderByIdAsc(1L)).thenReturn(List.of(meter1, meter2));
        when(meterReadingRepository.findTop2ByMeter_IdOrderByReadingDateDescIdDesc(10L))
                .thenReturn(List.of(m1curr, m1prev));
        when(meterReadingRepository.findTop2ByMeter_IdOrderByReadingDateDescIdDesc(20L))
                .thenReturn(List.of(m2curr, m2prev));

        // meter1 pair is duplicate, meter2 pair is new
        when(invoiceRepository.existsByCustomer_IdAndPreviousReadingRecord_IdAndCurrentReadingRecord_Id(
                1L, 1L, 2L)).thenReturn(true);
        when(invoiceRepository.existsByCustomer_IdAndPreviousReadingRecord_IdAndCurrentReadingRecord_Id(
                1L, 3L, 4L)).thenReturn(false);

        when(invoiceRepository.existsByInvoiceNumber(anyString())).thenReturn(false);
        when(tariffService.calculateAmount(any(TariffType.class), any(BigDecimal.class), any(LocalDate.class)))
                .thenReturn(tariffCalculation(new BigDecimal("400.00")));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            setId(i, 99L);
            return i;
        });

        List<InvoiceResponse> result = service.generateForCustomer(1L);

        assertThat(result).hasSize(1);
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    // ---------- validation: negative consumption ----------

    @Test
    @DisplayName("Negative consumption throws 422")
    void negativeConsumption_throws422() {
        // current < previous — invalid
        MeterReading prev = reading(1L, meter1, LocalDate.of(2024, 1, 1), new BigDecimal("150.00"));
        MeterReading curr = reading(2L, meter1, LocalDate.of(2024, 2, 1), new BigDecimal("100.00"));

        when(customerService.getCustomerOrThrow(1L)).thenReturn(customer);
        when(meterRepository.findByCustomer_IdOrderByIdAsc(1L)).thenReturn(List.of(meter1));
        when(meterReadingRepository.findTop2ByMeter_IdOrderByReadingDateDescIdDesc(10L))
                .thenReturn(List.of(curr, prev));
        when(invoiceRepository.existsByCustomer_IdAndPreviousReadingRecord_IdAndCurrentReadingRecord_Id(
                anyLong(), anyLong(), anyLong())).thenReturn(false);

        assertThatThrownBy(() -> service.generateForCustomer(1L))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("negative consumption");
    }

    // ---------- helpers ----------

    private static MeterReading reading(Long id, Meter meter, LocalDate date, BigDecimal value) {
        MeterReading r = MeterReading.builder().readingDate(date).readingValue(value).meter(meter).build();
        setId(r, id);
        return r;
    }

    private static Invoice savedInvoice(Customer customer, MeterReading prev, MeterReading curr,
                                        BigDecimal units, BigDecimal amount) {
        Invoice i = Invoice.builder()
                .invoiceNumber("INV-2024-TESTINV")
                .customer(customer)
                .previousReading(prev.getReadingValue())
                .currentReading(curr.getReadingValue())
                .unitsConsumed(units)
                .amount(amount)
                .generatedDate(LocalDate.now())
                .status(InvoiceStatus.GENERATED)
                .previousReadingRecord(prev)
                .currentReadingRecord(curr)
                .build();
        setId(i, 100L);
        return i;
    }

    private static TariffCalculation tariffCalculation(BigDecimal amount) {
        TariffPlan plan = TariffPlan.builder()
                .type(TariffType.RESIDENTIAL)
                .version(1)
                .name("Residential v1")
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .active(true)
                .build();
        setId(plan, 1L);
        return new TariffCalculation(amount, plan);
    }

    /** Reflectively sets the id field on BaseEntity subclasses (no setter on @Id field). */
    private static void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field f = findIdField(entity.getClass());
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Could not set id on " + entity.getClass().getSimpleName(), e);
        }
    }

    private static java.lang.reflect.Field findIdField(Class<?> clazz) throws NoSuchFieldException {
        Class<?> c = clazz;
        while (c != null) {
            try {
                return c.getDeclaredField("id");
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException("id not found in hierarchy of " + clazz.getName());
    }
}
