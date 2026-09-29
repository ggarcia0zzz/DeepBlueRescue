package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.AdmitRescueDto;
import com.deepblue.rescue.dto.ChangeRescueStatusDto;
import com.deepblue.rescue.dto.RescueCaseDto;
import jakarta.validation.Valid;

import java.util.List;

public interface RescueCaseService {

    RescueCaseDto admit(@Valid AdmitRescueDto request);

    RescueCaseDto findById(Long id);

    RescueCaseDto findByCaseCode(String caseCode);

    List<RescueCaseDto> findByStatus(RescueStatus status);

    List<RescueCaseDto> findByCenter(String centerCode);

    RescueCaseDto changeStatus(Long id, @Valid ChangeRescueStatusDto request);
}
