package com.deepblue.rescue.mapper;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.dto.AnimalDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = MedicalRecordMapper.class
)
public interface AnimalMapper {

    AnimalDto toDto(Animal animal);
}
