package com.ecovolt.billing.reading;

import com.ecovolt.billing.exception.BillingException;
import com.ecovolt.billing.meter.Meter;
import com.ecovolt.billing.meter.MeterService;
import com.ecovolt.billing.reading.dto.MeterReadingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeterReadingServiceTest {

    @Mock MeterReadingRepository meterReadingRepository;
    @Mock MeterService meterService;

    @InjectMocks MeterReadingService service;

    private Meter meter;

    @BeforeEach
    void setUp() {
        meter = Meter.builder().meterNumber("MTR-READING").build();
        setId(meter, 10L);
    }

    @Test
    @DisplayName("Duplicate reading date for same meter is rejected")
    void duplicateReadingDate_rejected() {
        MeterReadingRequest request = request("2024-02-01", "120.00");
        when(meterService.getMeterOrThrow(10L)).thenReturn(meter);
        when(meterReadingRepository.existsByMeter_IdAndReadingDate(10L, request.readingDate()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("already exists");

        verify(meterReadingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reading below previous chronological value is rejected")
    void readingBelowPrevious_rejected() {
        MeterReadingRequest request = request("2024-02-01", "90.00");
        MeterReading previous = reading("2024-01-01", "100.00");
        when(meterService.getMeterOrThrow(10L)).thenReturn(meter);
        when(meterReadingRepository.findFirstByMeter_IdAndReadingDateBeforeOrderByReadingDateDescIdDesc(
                10L, request.readingDate())).thenReturn(Optional.of(previous));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("lower than previous reading");

        verify(meterReadingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reading above next chronological value is rejected")
    void readingAboveNext_rejected() {
        MeterReadingRequest request = request("2024-02-01", "160.00");
        MeterReading next = reading("2024-03-01", "150.00");
        when(meterService.getMeterOrThrow(10L)).thenReturn(meter);
        when(meterReadingRepository.findFirstByMeter_IdAndReadingDateBeforeOrderByReadingDateDescIdDesc(
                10L, request.readingDate())).thenReturn(Optional.empty());
        when(meterReadingRepository.findFirstByMeter_IdAndReadingDateAfterOrderByReadingDateAscIdAsc(
                10L, request.readingDate())).thenReturn(Optional.of(next));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("higher than next reading");

        verify(meterReadingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Valid monotonic reading is saved")
    void validReading_saved() {
        MeterReadingRequest request = request("2024-02-01", "120.00");
        MeterReading saved = reading("2024-02-01", "120.00");
        setId(saved, 20L);
        when(meterService.getMeterOrThrow(10L)).thenReturn(meter);
        when(meterReadingRepository.findFirstByMeter_IdAndReadingDateBeforeOrderByReadingDateDescIdDesc(
                10L, request.readingDate())).thenReturn(Optional.of(reading("2024-01-01", "100.00")));
        when(meterReadingRepository.findFirstByMeter_IdAndReadingDateAfterOrderByReadingDateAscIdAsc(
                10L, request.readingDate())).thenReturn(Optional.of(reading("2024-03-01", "150.00")));
        when(meterReadingRepository.save(any(MeterReading.class))).thenReturn(saved);

        assertThat(service.create(request).readingValue()).isEqualByComparingTo("120.00");
    }

    private static MeterReadingRequest request(String date, String value) {
        return new MeterReadingRequest(10L, LocalDate.parse(date), new BigDecimal(value));
    }

    private MeterReading reading(String date, String value) {
        return MeterReading.builder()
                .meter(meter)
                .readingDate(LocalDate.parse(date))
                .readingValue(new BigDecimal(value))
                .build();
    }

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
