package com.deepblue.rescue.service;

import com.deepblue.rescue.dto.AddExpertiseDto;
import com.deepblue.rescue.dto.RegisterSpecialistDto;
import com.deepblue.rescue.dto.SpecialistDto;
import jakarta.validation.Valid;

import java.util.List;

public interface SpecialistService {

    SpecialistDto register(@Valid RegisterSpecialistDto request);

    SpecialistDto findById(Long id);

    List<SpecialistDto> findAll();

    List<SpecialistDto> findActiveByExpertise(String expertiseName);

    SpecialistDto addExpertise(Long id, @Valid AddExpertiseDto request);

    SpecialistDto activate(Long id);

    SpecialistDto deactivate(Long id);
}
