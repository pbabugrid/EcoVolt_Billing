package com.ecovolt.billing.meter;

import com.ecovolt.billing.meter.dto.MeterRequest;
import com.ecovolt.billing.meter.dto.MeterResponse;
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
@RequestMapping("/api/meters")
@RequiredArgsConstructor
@Tag(name = "Meters", description = "Meter registration and assignment")
public class MeterController {

    private final MeterService meterService;

    @PostMapping
    @Operation(summary = "Register a meter and assign it to a customer")
    public ResponseEntity<MeterResponse> create(@Valid @RequestBody MeterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(meterService.create(request));
    }

    @GetMapping
    @Operation(summary = "List all meters")
    public List<MeterResponse> findAll() {
        return meterService.findAll();
    }
}
