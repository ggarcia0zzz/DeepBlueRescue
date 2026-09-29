package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.dto.CreateRescueCenterDto;
import com.deepblue.rescue.dto.RescueCenterDto;
import com.deepblue.rescue.exception.DuplicateResourceException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCenterMapper;
import com.deepblue.rescue.repository.RescueCenterRepository;
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
public class RescueCenterServiceImpl implements RescueCenterService {

    private final RescueCenterRepository rescueCenterRepository;

    private final RescueCenterMapper rescueCenterMapper;

    @Override
    @Transactional
    public RescueCenterDto create(CreateRescueCenterDto request) {

        String code = TextNormalizer.code(request.code());

        if (rescueCenterRepository.existsByCode(code)) {
            throw new DuplicateResourceException("RescueCenter", "code", code);
        }

        RescueCenter center = new RescueCenter(
                code,
                TextNormalizer.text(request.name()),
                TextNormalizer.text(request.city())
        );

        return rescueCenterMapper.toDto(rescueCenterRepository.save(center));
    }

    @Override
    public RescueCenterDto findById(Long id) {
        return rescueCenterRepository
                .findById(id)
                .map(rescueCenterMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("RescueCenter", id));
    }

    @Override
    public RescueCenterDto findByCode(String code) {
        String normalized = TextNormalizer.code(code);

        return rescueCenterRepository
                .findByCode(normalized)
                .map(rescueCenterMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("RescueCenter", normalized));
    }

    @Override
    public List<RescueCenterDto> findAll() {
        return rescueCenterRepository
                .findAll()
                .stream()
                .map(rescueCenterMapper::toDto)
                .toList();
    }
}
