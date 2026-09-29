package com.deepblue.rescue.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateMedicalRecordDto(

        // DECIMAL(6,2) en la BD: máximo 4 enteros y 2 decimales
        @NotNull @DecimalMin("0.01") @Digits(integer = 4, fraction = 2) BigDecimal initialWeight,

        @NotBlank @Size(max = 30) String initialCondition,

        String injuries,

        String observations
) {
}
