package com.deepblue.rescue.mapper;

import com.deepblue.rescue.domain.Expertise;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.dto.SpecialistDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SpecialistMapper {

    SpecialistDto toDto(Specialist specialist);
    default String expertiseToName(Expertise expertise) {
        return expertise.getName();
    }
}
