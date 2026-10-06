package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.AnimalDto;
import com.deepblue.rescue.dto.AssignTrackingDeviceDto;
import com.deepblue.rescue.dto.TreatmentDto;
import com.deepblue.rescue.dto.TreatmentEligibilityDto;
import com.deepblue.rescue.service.AnimalService;
import com.deepblue.rescue.service.TreatmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/animals")
@RequiredArgsConstructor
public class AnimalController {

    private final AnimalService animalService;

    private final TreatmentService treatmentService;

    @GetMapping("/{id}")
    public ResponseEntity<AnimalDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(animalService.findById(id));
    }

    @GetMapping("/code/{animalCode}")
    public ResponseEntity<AnimalDto> findByCode(@PathVariable String animalCode) {
        return ResponseEntity.ok(animalService.findByCode(animalCode));
    }

    @GetMapping(params = "commonName")
    public ResponseEntity<List<AnimalDto>> searchByCommonName(@RequestParam String commonName) {
        return ResponseEntity.ok(animalService.searchByCommonName(commonName));
    }

    @GetMapping(params = "status")
    public ResponseEntity<List<AnimalDto>> findByStatus(@RequestParam RescueStatus status) {
        return ResponseEntity.ok(animalService.findByStatus(status));
    }

    // Más específico que el anterior (exige 2 parámetros), así que gana cuando vienen ambos
    @GetMapping(params = {"status", "expertise"})
    public ResponseEntity<List<AnimalDto>> findByStatusAndExpertise(@RequestParam RescueStatus status,
                                                                    @RequestParam String expertise) {
        return ResponseEntity.ok(animalService.findByStatusAndSpecialistExpertise(status, expertise));
    }

    @PatchMapping("/{id}/tracking-device")
    public ResponseEntity<AnimalDto> assignTrackingDevice(@PathVariable Long id,
                                                          @Valid @RequestBody AssignTrackingDeviceDto request) {
        return ResponseEntity.ok(animalService.assignTrackingDevice(id, request));
    }

    // Recurso anidado: los tratamientos de un animal
    @GetMapping("/{id}/treatments")
    public ResponseEntity<List<TreatmentDto>> findTreatments(@PathVariable Long id) {
        return ResponseEntity.ok(treatmentService.findByAnimal(id));
    }

    // Reto del laboratorio. Requiere el paso 9 (nuevo método en AnimalService)
    @GetMapping("/{animalCode}/treatment-eligibility")
    public ResponseEntity<TreatmentEligibilityDto> canReceiveTreatment(@PathVariable String animalCode) {
        boolean eligible = animalService.canReceiveTreatment(animalCode);
        return ResponseEntity.ok(new TreatmentEligibilityDto(animalCode, eligible));
    }
}
