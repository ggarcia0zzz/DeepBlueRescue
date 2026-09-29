package com.deepblue.rescue.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RegisterSpecialistDto(@NotBlank @Size(max = 50) String professionalCode,
                                    @NotBlank @Size(max = 100) String firstName,
                                    @NotBlank @Size(max = 100) String lastName,
                                    @NotBlank @Email @Size(max = 150) String email,
                                    @NotEmpty Set<@NotBlank String> expertiseNames) {
}
