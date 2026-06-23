package com.ecovolt.billing.reading;


import com.ecovolt.billing.reading.dto.MeterReadingRequest;
import com.ecovolt.billing.reading.dto.MeterReadingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/readings")
@RequiredArgsConstructor
@Tag(name = "Meter Readings", description = "Capture of periodic meter readings")
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    @PostMapping
    @Operation(summary = "Record a meter reading")
    public ResponseEntity<MeterReadingResponse> create(@Valid @RequestBody MeterReadingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(meterReadingService.create(request));
    }

    @GetMapping
    @Operation(summary = "List all meter readings")
    public List<MeterReadingResponse> findAll() {
        return meterReadingService.findAll();
    }
}
