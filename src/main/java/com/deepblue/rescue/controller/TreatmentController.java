package com.deepblue.rescue.controller;

import com.deepblue.rescue.dto.RegisterTreatmentDto;
import com.deepblue.rescue.dto.TreatmentDto;
import com.deepblue.rescue.service.TreatmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/treatments")
@RequiredArgsConstructor
public class TreatmentController {

    private final TreatmentService service;

    @PostMapping
    public ResponseEntity<TreatmentDto> register(@Valid @RequestBody RegisterTreatmentDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TreatmentDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    // Ejemplo: GET /api/treatments?start=2026-08-01T00:00:00&end=2026-08-31T23:59:59
    @GetMapping(params = {"start", "end"})
    public ResponseEntity<List<TreatmentDto>> findBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(service.findBetween(start, end));
    }
}
