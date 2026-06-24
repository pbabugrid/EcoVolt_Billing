package com.ecovolt.billing.meter;

import com.ecovolt.billing.meter.dto.MeterRequest;
import com.ecovolt.billing.meter.dto.MeterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @Operation(summary = "List meters with pagination")
    public Page<MeterResponse> findAll(@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return meterService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a meter by id")
    public MeterResponse findById(@PathVariable Long id) {
        return meterService.findById(id);
    }
}
