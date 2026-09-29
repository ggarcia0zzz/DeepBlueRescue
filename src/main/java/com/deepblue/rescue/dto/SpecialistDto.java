package com.deepblue.rescue.dto;

import java.util.Set;

public record SpecialistDto(Long id,
                            String professionalCode,
                            String firstName,
                            String lastName,
                            String email,
                            boolean active,
                            Set<String> expertiseAreas) {
}
