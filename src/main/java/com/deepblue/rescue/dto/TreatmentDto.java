package com.deepblue.rescue.dto;

import com.deepblue.rescue.domain.TreatmentType;

import java.time.LocalDateTime;

public record TreatmentDto(Long id,
                           Long animalId,
                           String animalCode,
                           Long specialistId,
                           String specialistName,
                           LocalDateTime performedAt,
                           TreatmentType type,
                           String description) {
}
