package com.deepblue.rescue.mapper;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.dto.RescueCaseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = AnimalMapper.class
)
public interface RescueCaseMapper {

    @Mapping(target = "centerCode", source = "rescueCenter.code")
    @Mapping(target = "centerName", source = "rescueCenter.name")
    RescueCaseDto toDto(RescueCase rescueCase);
}
