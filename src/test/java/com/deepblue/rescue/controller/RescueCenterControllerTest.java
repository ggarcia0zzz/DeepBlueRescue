package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.CreateRescueCenterDto;
import com.deepblue.rescue.dto.RescueCaseDto;
import com.deepblue.rescue.dto.RescueCenterDto;
import com.deepblue.rescue.exception.DuplicateResourceException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.RescueCaseService;
import com.deepblue.rescue.service.RescueCenterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RescueCenterController.class)
class RescueCenterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RescueCenterService centerService;

    @MockitoBean
    private RescueCaseService caseService;

    private RescueCenterDto center() {
        return new RescueCenterDto(1L, "DB-CAR", "DeepBlue Caribe", "Santa Marta");
    }

    @Test
    void shouldCreateCenter() throws Exception {
        when(centerService.create(new CreateRescueCenterDto("DB-CAR", "DeepBlue Caribe", "Santa Marta")))
                .thenReturn(center());

        mockMvc.perform(post("/api/rescue-centers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "DB-CAR", "name": "DeepBlue Caribe", "city": "Santa Marta" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DB-CAR"));

        verify(centerService).create(new CreateRescueCenterDto("DB-CAR", "DeepBlue Caribe", "Santa Marta"));
    }

    @Test
    void shouldReturn400WhenCenterBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/rescue-centers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "", "name": "", "city": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details.code").exists())
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.city").exists());

        verify(centerService, never()).create(any());
    }

    @Test
    void shouldReturn409WhenCenterCodeIsDuplicated() throws Exception {
        when(centerService.create(any()))
                .thenThrow(new DuplicateResourceException("RescueCenter", "code", "DB-CAR"));

        mockMvc.perform(post("/api/rescue-centers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "DB-CAR", "name": "DeepBlue Caribe", "city": "Santa Marta" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("RescueCenter already exists with code: DB-CAR"));
    }

    @Test
    void shouldReturnAllCenters() throws Exception {
        when(centerService.findAll()).thenReturn(List.of(center()));

        mockMvc.perform(get("/api/rescue-centers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(centerService).findAll();
    }

    @Test
    void shouldReturnCenterById() throws Exception {
        when(centerService.findById(1L)).thenReturn(center());

        mockMvc.perform(get("/api/rescue-centers/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("DeepBlue Caribe"));

        verify(centerService).findById(1L);
    }

    @Test
    void shouldReturn404WhenCenterIdDoesNotExist() throws Exception {
        when(centerService.findById(99L)).thenThrow(new ResourceNotFoundException("RescueCenter", 99L));

        mockMvc.perform(get("/api/rescue-centers/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("RescueCenter not found: 99"));
    }

    @Test
    void shouldReturnCenterByCode() throws Exception {
        when(centerService.findByCode("DB-CAR")).thenReturn(center());

        mockMvc.perform(get("/api/rescue-centers/code/{code}", "DB-CAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DB-CAR"));

        verify(centerService).findByCode("DB-CAR");
    }

    @Test
    void shouldReturnCasesOfACenter() throws Exception {
        RescueCaseDto dto = new RescueCaseDto(1L, "RES-2026-001", LocalDate.of(2026, 8, 20),
                "Bahia Concha", RescueStatus.ADMITTED, "DB-CAR", "DeepBlue Caribe", null);
        when(caseService.findByCenter("DB-CAR")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/rescue-centers/{code}/rescue-cases", "DB-CAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].centerCode").value("DB-CAR"));

        verify(caseService).findByCenter("DB-CAR");
    }

    @Test
    void shouldReturn404WhenCenterOfCasesDoesNotExist() throws Exception {
        when(caseService.findByCenter("DB-XXX"))
                .thenThrow(new ResourceNotFoundException("RescueCenter", "DB-XXX"));

        mockMvc.perform(get("/api/rescue-centers/{code}/rescue-cases", "DB-XXX"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("RescueCenter not found: DB-XXX"));
    }
}
