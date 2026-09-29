package com.deepblue.rescue.dto;

import jakarta.validation.constraints.NotBlank;

public record AddExpertiseDto(@NotBlank String expertiseName) {
}
