package com.ecovolt.billing.reading;


import com.ecovolt.billing.reading.dto.MeterReadingRequest;
import com.ecovolt.billing.reading.dto.MeterReadingResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @Operation(summary = "List meter readings with pagination")
    public Page<MeterReadingResponse> findAll(@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return meterReadingService.findAll(pageable);
    }
}
