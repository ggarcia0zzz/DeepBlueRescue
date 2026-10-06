package com.deepblue.rescue.controller;

import com.deepblue.rescue.dto.AddExpertiseDto;
import com.deepblue.rescue.dto.SpecialistDto;
import com.deepblue.rescue.exception.DuplicateResourceException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.SpecialistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpecialistController.class)
class SpecialistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpecialistService service;

    private SpecialistDto elena(boolean active) {
        return new SpecialistDto(1L, "SPEC-001", "Elena", "Vargas", "elena@deepblue.org",
                active, Set.of("Surgery"));
    }

    // ---------- POST /api/specialists ----------

    @Test
    void shouldRegisterSpecialist() throws Exception {
        when(service.register(any())).thenReturn(elena(true));

        mockMvc.perform(post("/api/specialists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "professionalCode": "SPEC-001",
                                  "firstName": "Elena",
                                  "lastName": "Vargas",
                                  "email": "elena@deepblue.org",
                                  "expertiseNames": ["Surgery"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.professionalCode").value("SPEC-001"))
                .andExpect(jsonPath("$.active").value(true));

        verify(service).register(any());
    }

    @Test
    void shouldReturn400WhenSpecialistBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/specialists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "professionalCode": "",
                                  "firstName": "Elena",
                                  "lastName": "Vargas",
                                  "email": "not-an-email",
                                  "expertiseNames": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details.professionalCode").exists())
                .andExpect(jsonPath("$.details.email").exists())
                .andExpect(jsonPath("$.details.expertiseNames").exists());

        verify(service, never()).register(any());
    }

    @Test
    void shouldReturn409WhenSpecialistIsDuplicated() throws Exception {
        when(service.register(any()))
                .thenThrow(new DuplicateResourceException("Specialist", "professionalCode", "SPEC-001"));

        mockMvc.perform(post("/api/specialists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "professionalCode": "SPEC-001",
                                  "firstName": "Elena",
                                  "lastName": "Vargas",
                                  "email": "elena@deepblue.org",
                                  "expertiseNames": ["Surgery"]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Specialist already exists with professionalCode: SPEC-001"));
    }

    // ---------- GET ----------

    @Test
    void shouldReturnAllSpecialists() throws Exception {
        when(service.findAll()).thenReturn(List.of(elena(true), elena(false)));

        mockMvc.perform(get("/api/specialists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(service).findAll();
    }

    @Test
    void shouldReturnActiveSpecialistsByExpertise() throws Exception {
        when(service.findActiveByExpertise("Surgery")).thenReturn(List.of(elena(true)));

        mockMvc.perform(get("/api/specialists").param("expertise", "Surgery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(service).findActiveByExpertise("Surgery");
        verify(service, never()).findAll();
    }

    @Test
    void shouldReturnSpecialistById() throws Exception {
        when(service.findById(1L)).thenReturn(elena(true));

        mockMvc.perform(get("/api/specialists/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Vargas"));

        verify(service).findById(1L);
    }

    @Test
    void shouldReturn404WhenSpecialistDoesNotExist() throws Exception {
        when(service.findById(99L)).thenThrow(new ResourceNotFoundException("Specialist", 99L));

        mockMvc.perform(get("/api/specialists/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Specialist not found: 99"));
    }

    // ---------- POST /api/specialists/{id}/expertise ----------

    @Test
    void shouldAddExpertise() throws Exception {
        when(service.addExpertise(1L, new AddExpertiseDto("Nutrition"))).thenReturn(elena(true));

        mockMvc.perform(post("/api/specialists/{id}/expertise", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "expertiseName": "Nutrition" }
                                """))
                .andExpect(status().isOk());

        verify(service).addExpertise(1L, new AddExpertiseDto("Nutrition"));
    }

    @Test
    void shouldReturn400WhenExpertiseNameIsBlank() throws Exception {
        mockMvc.perform(post("/api/specialists/{id}/expertise", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "expertiseName": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.expertiseName").exists());

        verify(service, never()).addExpertise(anyLong(), any());
    }

    // ---------- PATCH activate / deactivate ----------

    @Test
    void shouldActivateSpecialist() throws Exception {
        when(service.activate(1L)).thenReturn(elena(true));

        mockMvc.perform(patch("/api/specialists/{id}/activate", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        verify(service).activate(1L);
    }

    @Test
    void shouldDeactivateSpecialist() throws Exception {
        when(service.deactivate(1L)).thenReturn(elena(false));

        mockMvc.perform(patch("/api/specialists/{id}/deactivate", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(service).deactivate(1L);
    }

    @Test
    void shouldReturn404WhenActivatingUnknownSpecialist() throws Exception {
        when(service.activate(99L)).thenThrow(new ResourceNotFoundException("Specialist", 99L));

        mockMvc.perform(patch("/api/specialists/{id}/activate", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
