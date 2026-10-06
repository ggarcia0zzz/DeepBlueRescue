package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.AnimalDto;
import com.deepblue.rescue.dto.AssignTrackingDeviceDto;
import com.deepblue.rescue.exception.DuplicateResourceException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.shared.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class AnimalServiceImpl implements AnimalService {

    private final AnimalRepository animalRepository;

    private final AnimalMapper animalMapper;

    @Override
    public AnimalDto findById(Long id) {
        return animalMapper.toDto(findAnimal(id));
    }

    @Override
    public AnimalDto findByCode(String animalCode) {
        String code = TextNormalizer.code(animalCode);

        return animalRepository
                .findByAnimalCode(code)
                .map(animalMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Animal", code));
    }

    @Override
    public List<AnimalDto> searchByCommonName(String commonName) {
        return animalRepository
                .findByCommonNameContainingIgnoreCase(TextNormalizer.text(commonName))
                .stream()
                .map(animalMapper::toDto)
                .toList();
    }

    @Override
    public List<AnimalDto> findByStatus(RescueStatus status) {
        return animalRepository
                .findByRescueCase_Status(status)
                .stream()
                .map(animalMapper::toDto)
                .toList();
    }

    @Override
    public List<AnimalDto> findByStatusAndSpecialistExpertise(RescueStatus status, String expertiseName) {
        return animalRepository
                .findByRescueStatusAndSpecialistExpertise(status, TextNormalizer.text(expertiseName))
                .stream()
                .map(animalMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public AnimalDto assignTrackingDevice(Long animalId, AssignTrackingDeviceDto request) {

        Animal animal = findAnimal(animalId);
        String deviceCode = TextNormalizer.code(request.trackingDeviceCode());

        // R-A1: un dispositivo GPS no puede estar asignado a dos animales (UNIQUE en V3)
        boolean changingDevice = !deviceCode.equals(animal.getTrackingDeviceCode());

        if (changingDevice && animalRepository.existsByTrackingDeviceCode(deviceCode)) {
            throw new DuplicateResourceException("Animal", "trackingDeviceCode", deviceCode);
        }

        animal.assignTrackingDeviceCode(deviceCode);

        return animalMapper.toDto(animal);
    }

    @Override
    public boolean canReceiveTreatment(String animalCode) {
        String code = TextNormalizer.code(animalCode);

        Animal animal = animalRepository
                .findByAnimalCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Animal", code));

        // Misma regla R-T2 de TreatmentServiceImpl: el caso debe seguir bajo cuidado
        return animal.getRescueCase().getStatus().isUnderCare();
    }

    private Animal findAnimal(Long id) {
        return animalRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Animal", id));
    }
}