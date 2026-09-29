package com.deepblue.rescue.dto;

import java.math.BigDecimal;

public record MedicalRecordDto(
        Long id,
        BigDecimal initialWeight,
        String initialCondition,
        String injuries,
        String observations
) {
}
