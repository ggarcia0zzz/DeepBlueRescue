package com.deepblue.rescue.mapper;

import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.dto.RescueCenterDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RescueCenterMapper {

    RescueCenterDto toDto(RescueCenter center);
}
