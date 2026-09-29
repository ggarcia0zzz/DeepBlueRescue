package com.deepblue.rescue.mapper;

import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.dto.TreatmentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TreatmentMapper {

    @Mapping(target = "animalId", source = "animal.id")
    @Mapping(target = "animalCode", source = "animal.animalCode")
    @Mapping(target = "specialistId", source = "specialist.id")
    @Mapping(target = "specialistName",
            expression = "java(treatment.getSpecialist().getFirstName() + \" \" + treatment.getSpecialist().getLastName())")
    TreatmentDto toDto(Treatment treatment);
}
