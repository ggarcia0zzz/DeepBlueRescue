package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Expertise;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.dto.AddExpertiseDto;
import com.deepblue.rescue.dto.RegisterSpecialistDto;
import com.deepblue.rescue.dto.SpecialistDto;
import com.deepblue.rescue.exception.DuplicateResourceException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.SpecialistMapper;
import com.deepblue.rescue.repository.ExpertiseRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.shared.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class SpecialistServiceImpl implements SpecialistService {

    private final SpecialistRepository specialistRepository;

    private final ExpertiseRepository expertiseRepository;

    private final SpecialistMapper specialistMapper;

    @Override
    @Transactional
    public SpecialistDto register(RegisterSpecialistDto request) {

        String email = TextNormalizer.email(request.email());
        String professionalCode = TextNormalizer.code(request.professionalCode());

        if (specialistRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Specialist", "email", email);
        }

        if (specialistRepository.existsByProfessionalCode(professionalCode)) {
            throw new DuplicateResourceException("Specialist", "professionalCode", professionalCode);
        }

        // Cada expertise debe existir en el catálogo (tabla expertise)
        Set<Expertise> areas = request.expertiseNames()
                .stream()
                .map(TextNormalizer::text)
                .map(this::findExpertise)
                .collect(Collectors.toSet());

        Specialist specialist = new Specialist(
                professionalCode,
                TextNormalizer.text(request.firstName()),
                TextNormalizer.text(request.lastName()),
                email,
                true
        );
        areas.forEach(specialist::addExpertise);

        return specialistMapper.toDto(specialistRepository.save(specialist));
    }

    @Override
    public SpecialistDto findById(Long id) {
        return specialistMapper.toDto(findSpecialist(id));
    }

    @Override
    public List<SpecialistDto> findAll() {
        return specialistRepository
                .findAll()
                .stream()
                .map(specialistMapper::toDto)
                .toList();
    }

    @Override
    public List<SpecialistDto> findActiveByExpertise(String expertiseName) {
        Expertise expertise = findExpertise(TextNormalizer.text(expertiseName));

        return specialistRepository
                .findActiveByExpertise(expertise.getName())
                .stream()
                .map(specialistMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public SpecialistDto addExpertise(Long id, AddExpertiseDto request) {
        Specialist specialist = findSpecialist(id);
        Expertise expertise = findExpertise(TextNormalizer.text(request.expertiseName()));

        specialist.addExpertise(expertise);   // es un Set: si ya la tenía, no pasa nada

        return specialistMapper.toDto(specialist);
    }

    @Override
    @Transactional
    public SpecialistDto activate(Long id) {
        Specialist specialist = findSpecialist(id);
        specialist.activate();
        return specialistMapper.toDto(specialist);
    }

    @Override
    @Transactional
    public SpecialistDto deactivate(Long id) {
        Specialist specialist = findSpecialist(id);
        specialist.deactivate();
        return specialistMapper.toDto(specialist);
    }

    private Specialist findSpecialist(Long id) {
        return specialistRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialist", id));
    }

    private Expertise findExpertise(String name) {
        return expertiseRepository
                .findByNameIgnoreCase(name)
                .orElseThrow(() -> new ResourceNotFoundException("Expertise", name));
    }
}
