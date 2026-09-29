package com.deepblue.rescue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRescueCenterDto(@NotBlank @Size(max = 50) String code,
                                    @NotBlank @Size(max = 150) String name,
                                    @NotBlank @Size(max = 100) String city) {
}
