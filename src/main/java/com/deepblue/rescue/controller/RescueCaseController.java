package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.AdmitRescueDto;
import com.deepblue.rescue.dto.ChangeRescueStatusDto;
import com.deepblue.rescue.dto.RescueCaseDto;
import com.deepblue.rescue.service.RescueCaseService;
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
@RequestMapping("/api/rescue-cases")
@RequiredArgsConstructor
public class RescueCaseController {

    private final RescueCaseService service;

    @PostMapping
    public ResponseEntity<RescueCaseDto> admit(@Valid @RequestBody AdmitRescueDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.admit(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RescueCaseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/code/{caseCode}")
    public ResponseEntity<RescueCaseDto> findByCaseCode(@PathVariable String caseCode) {
        return ResponseEntity.ok(service.findByCaseCode(caseCode));
    }

    @GetMapping(params = "status")
    public ResponseEntity<List<RescueCaseDto>> findByStatus(@RequestParam RescueStatus status) {
        return ResponseEntity.ok(service.findByStatus(status));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RescueCaseDto> changeStatus(@PathVariable Long id,
                                                      @Valid @RequestBody ChangeRescueStatusDto request) {
        return ResponseEntity.ok(service.changeStatus(id, request));
    }
}
