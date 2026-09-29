package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.dto.RegisterTreatmentDto;
import com.deepblue.rescue.dto.TreatmentDto;
import com.deepblue.rescue.exception.CaseNotUnderCareException;
import com.deepblue.rescue.exception.InactiveSpecialistException;
import com.deepblue.rescue.exception.InvalidTreatmentDateException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.shared.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class TreatmentServiceImpl implements TreatmentService {

    // OJO: el orden de estos campos define el orden del constructor (@RequiredArgsConstructor)
    private final TreatmentRepository treatmentRepository;

    private final AnimalRepository animalRepository;

    private final SpecialistRepository specialistRepository;

    private final TreatmentMapper treatmentMapper;

    private final Clock clock;

    @Override
    @Transactional
    public TreatmentDto register(RegisterTreatmentDto request) {

        Animal animal = animalRepository
                .findById(request.animalId())
                .orElseThrow(() -> new ResourceNotFoundException("Animal", request.animalId()));

        Specialist specialist = specialistRepository
                .findById(request.specialistId())
                .orElseThrow(() -> new ResourceNotFoundException("Specialist", request.specialistId()));

        // R-T1: solo especialistas activos atienden
        if (!specialist.isActive()) {
            throw new InactiveSpecialistException(specialist.getId());
        }

        // R-T2: el caso debe seguir bajo cuidado (no RELEASED ni CLOSED)
        RescueCase rescueCase = animal.getRescueCase();

        if (!rescueCase.getStatus().isUnderCare()) {
            throw new CaseNotUnderCareException(rescueCase.getCaseCode(), rescueCase.getStatus());
        }

        // R-T3: la fecha del tratamiento no puede ser futura ni anterior al rescate
        LocalDateTime performedAt = request.performedAt();

        if (performedAt.isAfter(LocalDateTime.now(clock))) {
            throw new InvalidTreatmentDateException(
                    "Treatment date cannot be in the future: " + performedAt);
        }

        if (performedAt.toLocalDate().isBefore(rescueCase.getRescueDate())) {
            throw new InvalidTreatmentDateException(
                    "Treatment date %s is before the rescue date %s"
                            .formatted(performedAt, rescueCase.getRescueDate()));
        }

        Treatment treatment = new Treatment(
                animal,
                specialist,
                performedAt,
                request.type(),
                TextNormalizer.optionalText(request.description())
        );

        return treatmentMapper.toDto(treatmentRepository.save(treatment));
    }

    @Override
    public TreatmentDto findById(Long id) {
        return treatmentRepository
                .findById(id)
                .map(treatmentMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment", id));
    }

    @Override
    public List<TreatmentDto> findByAnimal(Long animalId) {

        if (!animalRepository.existsById(animalId)) {
            throw new ResourceNotFoundException("Animal", animalId);
        }

        return treatmentRepository
                .findByAnimal_IdOrderByPerformedAtAsc(animalId)
                .stream()
                .map(treatmentMapper::toDto)
                .toList();
    }

    @Override
    public List<TreatmentDto> findBetween(LocalDateTime start, LocalDateTime end) {

        if (end.isBefore(start)) {
            throw new InvalidTreatmentDateException("End date is before start date: " + start + " > " + end);
        }

        return treatmentRepository
                .findByPerformedAtBetween(start, end)
                .stream()
                .map(treatmentMapper::toDto)
                .toList();
    }
}
