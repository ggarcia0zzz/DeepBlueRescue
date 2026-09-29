package com.deepblue.rescue.dto;

import com.deepblue.rescue.domain.RescueStatus;

import java.time.LocalDate;

public record RescueCaseDto(
        Long id,
        String caseCode,
        LocalDate rescueDate,
        String rescueLocation,
        RescueStatus status,
        String centerCode,
        String centerName,
        AnimalDto animal
) {
}
