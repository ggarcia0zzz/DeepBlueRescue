package com.deepblue.rescue.controller;

import com.deepblue.rescue.dto.AddExpertiseDto;
import com.deepblue.rescue.dto.RegisterSpecialistDto;
import com.deepblue.rescue.dto.SpecialistDto;
import com.deepblue.rescue.service.SpecialistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/specialists")
@RequiredArgsConstructor
public class SpecialistController {

    private final SpecialistService service;

    @PostMapping
    public ResponseEntity<SpecialistDto> register(@Valid @RequestBody RegisterSpecialistDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }

    @GetMapping
    public ResponseEntity<List<SpecialistDto>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping(params = "expertise")
    public ResponseEntity<List<SpecialistDto>> findActiveByExpertise(@RequestParam String expertise) {
        return ResponseEntity.ok(service.findActiveByExpertise(expertise));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpecialistDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping("/{id}/expertise")
    public ResponseEntity<SpecialistDto> addExpertise(@PathVariable Long id,
                                                      @Valid @RequestBody AddExpertiseDto request) {
        return ResponseEntity.ok(service.addExpertise(id, request));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<SpecialistDto> activate(@PathVariable Long id) {
        return ResponseEntity.ok(service.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<SpecialistDto> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(service.deactivate(id));
    }
}
