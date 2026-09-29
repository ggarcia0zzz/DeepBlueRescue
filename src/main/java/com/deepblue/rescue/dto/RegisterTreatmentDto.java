package com.deepblue.rescue.dto;

import com.deepblue.rescue.domain.TreatmentType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RegisterTreatmentDto(@NotNull Long animalId,
                                   @NotNull Long specialistId,
                                   @NotNull LocalDateTime performedAt,
                                   @NotNull TreatmentType type,
                                   String description) {
}
