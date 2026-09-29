package com.deepblue.rescue.dto;

import com.deepblue.rescue.domain.AnimalSex;

public record AnimalDto(
        Long id,
        String animalCode,
        String commonName,
        String scientificName,
        AnimalSex sex,
        String trackingDeviceCode,
        MedicalRecordDto medicalRecord
) {
}
