package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.MedicalRecord;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.AdmitRescueDto;
import com.deepblue.rescue.dto.ChangeRescueStatusDto;
import com.deepblue.rescue.dto.CreateMedicalRecordDto;
import com.deepblue.rescue.dto.RescueCaseDto;
import com.deepblue.rescue.exception.DuplicateResourceException;
import com.deepblue.rescue.exception.InvalidRescueDateException;
import com.deepblue.rescue.exception.ReleaseRequirementsNotMetException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.repository.RescueCenterRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.shared.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class RescueCaseServiceImpl implements RescueCaseService {

    // OJO: el orden de estos campos define el orden del constructor (@RequiredArgsConstructor)
    private final RescueCaseRepository rescueCaseRepository;

    private final RescueCenterRepository rescueCenterRepository;

    private final AnimalRepository animalRepository;

    private final TreatmentRepository treatmentRepository;

    private final RescueCaseMapper rescueCaseMapper;

    private final Clock clock;

    @Override
    @Transactional
    public RescueCaseDto admit(AdmitRescueDto request) {

        String caseCode = TextNormalizer.code(request.caseCode());
        String animalCode = TextNormalizer.code(request.animalCode());
        String centerCode = TextNormalizer.code(request.centerCode());
        String trackingDeviceCode = TextNormalizer.optionalCode(request.trackingDeviceCode());

        // R-C1: la fecha de rescate no puede estar en el futuro
        if (request.rescueDate().isAfter(LocalDate.now(clock))) {
            throw new InvalidRescueDateException(request.rescueDate());
        }

        // R-C2: códigos únicos
        if (rescueCaseRepository.existsByCaseCode(caseCode)) {
            throw new DuplicateResourceException("RescueCase", "caseCode", caseCode);
        }

        if (animalRepository.existsByAnimalCode(animalCode)) {
            throw new DuplicateResourceException("Animal", "animalCode", animalCode);
        }

        if (trackingDeviceCode != null
                && animalRepository.existsByTrackingDeviceCode(trackingDeviceCode)) {
            throw new DuplicateResourceException("Animal", "trackingDeviceCode", trackingDeviceCode);
        }

        // R-C3: el centro debe existir
        RescueCenter center = rescueCenterRepository
                .findByCode(centerCode)
                .orElseThrow(() -> new ResourceNotFoundException("RescueCenter", centerCode));

        // Se arma el grafo completo: Center -> Case -> Animal -> MedicalRecord
        RescueCase rescueCase = RescueCase.open(
                caseCode,
                request.rescueDate(),
                TextNormalizer.text(request.rescueLocation())
        );
        center.addCase(rescueCase);

        Animal animal = new Animal(
                animalCode,
                TextNormalizer.text(request.commonName()),
                TextNormalizer.text(request.scientificName()),
                request.sex()
        );
        if (trackingDeviceCode != null) {
            animal.assignTrackingDeviceCode(trackingDeviceCode);
        }

        CreateMedicalRecordDto record = request.medicalRecord();
        animal.assignMedicalRecord(new MedicalRecord(
                record.initialWeight(),
                TextNormalizer.code(record.initialCondition()),
                TextNormalizer.optionalText(record.injuries()),
                TextNormalizer.optionalText(record.observations())
        ));

        rescueCase.assignAnimal(animal);

        // Un solo save en la raíz: el cascade ALL baja a Animal y MedicalRecord
        RescueCase saved = rescueCaseRepository.save(rescueCase);

        return rescueCaseMapper.toDto(saved);
    }

    @Override
    public RescueCaseDto findById(Long id) {
        return rescueCaseMapper.toDto(findCase(id));
    }

    @Override
    public RescueCaseDto findByCaseCode(String caseCode) {
        String normalized = TextNormalizer.code(caseCode);

        return rescueCaseRepository
                .findByCaseCode(normalized)
                .map(rescueCaseMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("RescueCase", normalized));
    }

    @Override
    public List<RescueCaseDto> findByStatus(RescueStatus status) {
        return rescueCaseRepository
                .findAllByStatusOrderByRescueDateAsc(status)
                .stream()
                .map(rescueCaseMapper::toDto)
                .toList();
    }

    @Override
    public List<RescueCaseDto> findByCenter(String centerCode) {
        String code = TextNormalizer.code(centerCode);

        if (!rescueCenterRepository.existsByCode(code)) {
            throw new ResourceNotFoundException("RescueCenter", code);
        }

        return rescueCaseRepository
                .findAllByRescueCenter_Code(code)
                .stream()
                .map(rescueCaseMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public RescueCaseDto changeStatus(Long id, ChangeRescueStatusDto request) {

        RescueCase rescueCase = findCase(id);
        RescueStatus current = rescueCase.getStatus();
        RescueStatus target = request.newStatus();

        // R-C4: para quedar listo para liberación, el animal debe tener al menos un tratamiento.
        // Solo se evalúa si la transición es válida, así el error de transición siempre gana.
        if (target == RescueStatus.READY_FOR_RELEASE
                && current.canTransitionTo(target)
                && !hasTreatments(rescueCase)) {
            throw new ReleaseRequirementsNotMetException(rescueCase.getCaseCode());
        }

        // La entidad valida el flujo de estados (R-C5) y lanza InvalidStatusTransitionException
        rescueCase.changeStatus(target);

        // Sin save(): Dirty Checking persiste el cambio al terminar la transacción
        return rescueCaseMapper.toDto(rescueCase);
    }

    private RescueCase findCase(Long id) {
        return rescueCaseRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RescueCase", id));
    }

    private boolean hasTreatments(RescueCase rescueCase) {
        return rescueCase.getAnimal() != null
                && treatmentRepository.existsByAnimal_Id(rescueCase.getAnimal().getId());
    }
}
