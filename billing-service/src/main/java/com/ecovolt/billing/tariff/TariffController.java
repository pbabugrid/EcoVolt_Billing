package com.ecovolt.billing.tariff;

import com.ecovolt.billing.tariff.dto.TariffPlanRequest;
import com.ecovolt.billing.tariff.dto.TariffPlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
@RestController
@RequestMapping("/api/tariff-plans")
@RequiredArgsConstructor
@Tag(name = "Tariff Plans", description = "Slab-based tariff plans with versioned effective dates")
public class TariffController {

    private final TariffService tariffService;

    @PostMapping
    @Operation(summary = "Create a versioned tariff plan")
    public ResponseEntity<TariffPlanResponse> create(@Valid @RequestBody TariffPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tariffService.create(request));
    }

    @GetMapping
    @Operation(summary = "List tariff plans with pagination")
    public Page<TariffPlanResponse> findAll(@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return tariffService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a tariff plan by id")
    public TariffPlanResponse findById(@PathVariable Long id) {
        return tariffService.findById(id);
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a tariff plan and close its effective window")
    public TariffPlanResponse deactivate(
            @PathVariable Long id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate effectiveTo) {
        return tariffService.deactivate(id, effectiveTo);
    }
}
