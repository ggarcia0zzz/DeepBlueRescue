package com.deepblue.rescue.controller;

import com.deepblue.rescue.dto.CreateRescueCenterDto;
import com.deepblue.rescue.dto.RescueCaseDto;
import com.deepblue.rescue.dto.RescueCenterDto;
import com.deepblue.rescue.service.RescueCaseService;
import com.deepblue.rescue.service.RescueCenterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rescue-centers")
@RequiredArgsConstructor
public class RescueCenterController {

    private final RescueCenterService centerService;

    private final RescueCaseService caseService;

    @PostMapping
    public ResponseEntity<RescueCenterDto> create(@Valid @RequestBody CreateRescueCenterDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(centerService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<RescueCenterDto>> findAll() {
        return ResponseEntity.ok(centerService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RescueCenterDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(centerService.findById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<RescueCenterDto> findByCode(@PathVariable String code) {
        return ResponseEntity.ok(centerService.findByCode(code));
    }

    // Recurso anidado: los casos que pertenecen a un centro
    @GetMapping("/{code}/rescue-cases")
    public ResponseEntity<List<RescueCaseDto>> findCasesByCenter(@PathVariable String code) {
        return ResponseEntity.ok(caseService.findByCenter(code));
    }
}
