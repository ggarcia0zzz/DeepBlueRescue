package com.deepblue.rescue.service;

import com.deepblue.rescue.dto.RegisterTreatmentDto;
import com.deepblue.rescue.dto.TreatmentDto;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

public interface TreatmentService {

    TreatmentDto register(@Valid RegisterTreatmentDto request);

    TreatmentDto findById(Long id);

    List<TreatmentDto> findByAnimal(Long animalId);

    List<TreatmentDto> findBetween(LocalDateTime start, LocalDateTime end);
}
