package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.ChangeRescueStatusDto;
import com.deepblue.rescue.dto.RescueCaseDto;
import com.deepblue.rescue.exception.DuplicateResourceException;
import com.deepblue.rescue.exception.InvalidStatusTransitionException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.RescueCaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

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

@WebMvcTest(RescueCaseController.class)
class RescueCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RescueCaseService service;

    private static final String ADMIT_JSON = """
            {
              "caseCode": "RES-2026-001",
              "rescueDate": "2026-08-20",
              "rescueLocation": "Bahia Concha",
              "centerCode": "DB-CAR",
              "animalCode": "AN-2026-001",
              "commonName": "Green Sea Turtle",
              "scientificName": "Chelonia mydas",
              "sex": "FEMALE",
              "medicalRecord": {
                "initialWeight": 45.50,
                "initialCondition": "CRITICAL"
              }
            }
            """;

    private RescueCaseDto caseDto(RescueStatus status) {
        return new RescueCaseDto(1L, "RES-2026-001", LocalDate.of(2026, 8, 20),
                "Bahia Concha", status, "DB-CAR", "DeepBlue Caribe", null);
    }

    // ---------- POST /api/rescue-cases ----------

    @Test
    void shouldAdmitRescue() throws Exception {
        when(service.admit(any())).thenReturn(caseDto(RescueStatus.ADMITTED));

        mockMvc.perform(post("/api/rescue-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADMIT_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caseCode").value("RES-2026-001"))
                .andExpect(jsonPath("$.status").value("ADMITTED"));

        verify(service).admit(any());
    }

    @Test
    void shouldReturn400WhenAdmitBodyIsEmpty() throws Exception {
        mockMvc.perform(post("/api/rescue-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details.caseCode").exists())
                .andExpect(jsonPath("$.details.medicalRecord").exists());

        verify(service, never()).admit(any());
    }

    @Test
    void shouldReturn409WhenCaseCodeIsDuplicated() throws Exception {
        when(service.admit(any()))
                .thenThrow(new DuplicateResourceException("RescueCase", "caseCode", "RES-2026-001"));

        mockMvc.perform(post("/api/rescue-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADMIT_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("RescueCase already exists with caseCode: RES-2026-001"));
    }

    // ---------- GET /api/rescue-cases/{id} ----------

    @Test
    void shouldReturnRescueCaseById() throws Exception {
        when(service.findById(1L)).thenReturn(caseDto(RescueStatus.IN_REHABILITATION));

        mockMvc.perform(get("/api/rescue-cases/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("IN_REHABILITATION"));

        verify(service).findById(1L);
    }

    @Test
    void shouldReturn404WhenCaseDoesNotExist() throws Exception {
        when(service.findById(99L)).thenThrow(new ResourceNotFoundException("RescueCase", 99L));

        mockMvc.perform(get("/api/rescue-cases/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("RescueCase not found: 99"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void shouldReturn400WhenIdIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/rescue-cases/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameter"))
                .andExpect(jsonPath("$.details.id").exists());

        verify(service, never()).findById(anyLong());
    }

    // ---------- GET /api/rescue-cases/code/{caseCode} ----------

    @Test
    void shouldReturnRescueCaseByCode() throws Exception {
        when(service.findByCaseCode("RES-2026-001")).thenReturn(caseDto(RescueStatus.ADMITTED));

        mockMvc.perform(get("/api/rescue-cases/code/{code}", "RES-2026-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseCode").value("RES-2026-001"));

        verify(service).findByCaseCode("RES-2026-001");
    }

    @Test
    void shouldReturn404WhenCaseCodeDoesNotExist() throws Exception {
        when(service.findByCaseCode("RES-999"))
                .thenThrow(new ResourceNotFoundException("RescueCase", "RES-999"));

        mockMvc.perform(get("/api/rescue-cases/code/{code}", "RES-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("RescueCase not found: RES-999"));
    }

    // ---------- GET /api/rescue-cases?status= ----------

    @Test
    void shouldReturnCasesByStatus() throws Exception {
        when(service.findByStatus(RescueStatus.IN_REHABILITATION)).thenReturn(List.of(
                caseDto(RescueStatus.IN_REHABILITATION),
                caseDto(RescueStatus.IN_REHABILITATION)));

        mockMvc.perform(get("/api/rescue-cases").param("status", "IN_REHABILITATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("IN_REHABILITATION"));

        verify(service).findByStatus(RescueStatus.IN_REHABILITATION);
    }

    @Test
    void shouldReturn400WhenStatusParamIsInvalid() throws Exception {
        mockMvc.perform(get("/api/rescue-cases").param("status", "FLYING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid request parameter"))
                .andExpect(jsonPath("$.details.status").exists());

        verify(service, never()).findByStatus(any());
    }

    @Test
    void shouldReturn400WhenStatusParamIsMissing() throws Exception {
        // Prueba que el handler de Exception respeta el código HTTP propio de Spring (no lo vuelve 500)
        mockMvc.perform(get("/api/rescue-cases"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ---------- PATCH /api/rescue-cases/{id}/status ----------

    @Test
    void shouldChangeStatus() throws Exception {
        when(service.changeStatus(1L, new ChangeRescueStatusDto(RescueStatus.READY_FOR_RELEASE)))
                .thenReturn(caseDto(RescueStatus.READY_FOR_RELEASE));

        mockMvc.perform(patch("/api/rescue-cases/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "newStatus": "READY_FOR_RELEASE" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_RELEASE"));

        verify(service).changeStatus(1L, new ChangeRescueStatusDto(RescueStatus.READY_FOR_RELEASE));
    }

    @Test
    void shouldReturn400WhenNewStatusIsMissing() throws Exception {
        mockMvc.perform(patch("/api/rescue-cases/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details.newStatus").exists());

        verify(service, never()).changeStatus(anyLong(), any());
    }

    @Test
    void shouldReturn400WhenNewStatusIsNotAValidEnumValue() throws Exception {
        mockMvc.perform(patch("/api/rescue-cases/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "newStatus": "FLYING" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"))
                .andExpect(jsonPath("$.details.body").exists());

        verify(service, never()).changeStatus(anyLong(), any());
    }

    @Test
    void shouldReturn409OnInvalidStatusTransition() throws Exception {
        when(service.changeStatus(anyLong(), any()))
                .thenThrow(new InvalidStatusTransitionException(RescueStatus.ADMITTED, RescueStatus.RELEASED));

        mockMvc.perform(patch("/api/rescue-cases/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "newStatus": "RELEASED" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid rescue status transition: ADMITTED -> RELEASED"))
                .andExpect(jsonPath("$.details").isMap());
    }

    // ---------- 500 ----------

    @Test
    void shouldReturn500WithoutLeakingInternalDetails() throws Exception {
        when(service.findById(1L)).thenThrow(new RuntimeException("connection refused to db:5432"));

        mockMvc.perform(get("/api/rescue-cases/{id}", 1L))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
