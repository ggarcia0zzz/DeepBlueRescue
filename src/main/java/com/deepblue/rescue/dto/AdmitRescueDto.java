package com.deepblue.rescue.dto;

import com.deepblue.rescue.domain.AnimalSex;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Caso de uso "admitir un rescate": crea caso + animal + historia clínica en una sola operación. */
public record AdmitRescueDto(

        @NotBlank @Size(max = 50) String caseCode,

        @NotNull LocalDate rescueDate,

        @NotBlank @Size(max = 150) String rescueLocation,

        @NotBlank String centerCode,

        @NotBlank @Size(max = 50) String animalCode,

        @NotBlank @Size(max = 100) String commonName,

        @NotBlank @Size(max = 150) String scientificName,

        @NotNull AnimalSex sex,

        @Size(max = 50) String trackingDeviceCode,

        @NotNull @Valid CreateMedicalRecordDto medicalRecord
) {
}
