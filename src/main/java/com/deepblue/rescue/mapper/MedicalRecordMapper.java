package com.deepblue.rescue.mapper;

import com.deepblue.rescue.domain.MedicalRecord;
import com.deepblue.rescue.dto.MedicalRecordDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MedicalRecordMapper {

    MedicalRecordDto toDto(MedicalRecord medicalRecord);
}
