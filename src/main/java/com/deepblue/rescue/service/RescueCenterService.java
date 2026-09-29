package com.deepblue.rescue.service;

import com.deepblue.rescue.dto.CreateRescueCenterDto;
import com.deepblue.rescue.dto.RescueCenterDto;
import jakarta.validation.Valid;

import java.util.List;

public interface RescueCenterService {

    RescueCenterDto create(@Valid CreateRescueCenterDto request);

    RescueCenterDto findById(Long id);

    RescueCenterDto findByCode(String code);

    List<RescueCenterDto> findAll();
}
