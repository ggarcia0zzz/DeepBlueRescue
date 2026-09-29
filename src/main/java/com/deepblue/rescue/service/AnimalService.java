package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.AnimalDto;
import com.deepblue.rescue.dto.AssignTrackingDeviceDto;
import jakarta.validation.Valid;

import java.util.List;

public interface AnimalService {

    AnimalDto findById(Long id);

    AnimalDto findByCode(String animalCode);

    List<AnimalDto> searchByCommonName(String commonName);

    List<AnimalDto> findByStatus(RescueStatus status);

    List<AnimalDto> findByStatusAndSpecialistExpertise(RescueStatus status, String expertiseName);

    AnimalDto assignTrackingDevice(Long animalId, @Valid AssignTrackingDeviceDto request);
}
