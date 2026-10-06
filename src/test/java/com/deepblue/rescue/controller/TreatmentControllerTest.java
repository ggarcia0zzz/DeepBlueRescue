package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.RegisterTreatmentDto;
import com.deepblue.rescue.dto.TreatmentDto;
import com.deepblue.rescue.exception.CaseNotUnderCareException;
import com.deepblue.rescue.exception.InactiveSpecialistException;
import com.deepblue.rescue.exception.InvalidTreatmentDateException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.TreatmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TreatmentController.class)
class TreatmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TreatmentService service;

    private static final String VALID_JSON = """
            {
              "animalId": 1,
              "specialistId": 2,
              "performedAt": "2026-08-21T09:30:00",
              "type": "WOUND_CARE",
              "description": "Cleaning and evaluation of left front flipper injury."
            }
            """;

    private TreatmentDto treatment() {
        return new TreatmentDto(100L, 1L, "AN-2026-001", 2L, "Elena Vargas",
                LocalDateTime.of(2026, 8, 21, 9, 30), TreatmentType.WOUND_CARE,
                "Cleaning and evaluation of left front flipper injury.");
    }

    // ---------- POST /api/treatments ----------

    @Test
    void shouldRegisterTreatment() throws Exception {
        RegisterTreatmentDto expected = new RegisterTreatmentDto(1L, 2L,
                LocalDateTime.of(2026, 8, 21, 9, 30), TreatmentType.WOUND_CARE,
                "Cleaning and evaluation of left front flipper injury.");
        when(service.register(expected)).thenReturn(treatment());

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.type").value("WOUND_CARE"));

        // Verifica que el JSON se transformó en el DTO correcto antes de llegar al Service
        verify(service).register(expected);
    }

    @Test
    void shouldReturn400WhenTreatmentBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details.animalId").exists())
                .andExpect(jsonPath("$.details.specialistId").exists())
                .andExpect(jsonPath("$.details.performedAt").exists())
                .andExpect(jsonPath("$.details.type").exists());

        verify(service, never()).register(any());
    }

    @Test
    void shouldReturn400WhenTreatmentTypeIsNotAValidEnumValue() throws Exception {
        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON.replace("WOUND_CARE", "MAGIC")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"));

        verify(service, never()).register(any());
    }

    @Test
    void shouldReturn404WhenAnimalOfTreatmentDoesNotExist() throws Exception {
        when(service.register(any())).thenThrow(new ResourceNotFoundException("Animal", 1L));

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Animal not found: 1"));
    }

    @Test
    void shouldReturn409WhenSpecialistIsInactive() throws Exception {
        when(service.register(any())).thenThrow(new InactiveSpecialistException(2L));

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Specialist is inactive and cannot perform treatments: 2"));
    }

    @Test
    void shouldReturn409WhenCaseIsNoLongerUnderCare() throws Exception {
        when(service.register(any()))
                .thenThrow(new CaseNotUnderCareException("RES-2026-001", RescueStatus.RELEASED));

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Case RES-2026-001 is RELEASED and no longer accepts treatments"));
    }

    // ---------- GET /api/treatments/{id} ----------

    @Test
    void shouldReturnTreatmentById() throws Exception {
        when(service.findById(100L)).thenReturn(treatment());

        mockMvc.perform(get("/api/treatments/{id}", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialistName").value("Elena Vargas"));

        verify(service).findById(100L);
    }

    @Test
    void shouldReturn404WhenTreatmentDoesNotExist() throws Exception {
        when(service.findById(999L)).thenThrow(new ResourceNotFoundException("Treatment", 999L));

        mockMvc.perform(get("/api/treatments/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Treatment not found: 999"));
    }

    // ---------- GET /api/treatments?start=&end= ----------

    @Test
    void shouldReturnTreatmentsBetweenDates() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 31, 23, 59, 59);
        when(service.findBetween(start, end)).thenReturn(List.of(treatment()));

        mockMvc.perform(get("/api/treatments")
                        .param("start", "2026-08-01T00:00:00")
                        .param("end", "2026-08-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(service).findBetween(start, end);
    }

    @Test
    void shouldReturn400WhenDateParamIsInvalid() throws Exception {
        mockMvc.perform(get("/api/treatments")
                        .param("start", "yesterday")
                        .param("end", "2026-08-31T23:59:59"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameter"))
                .andExpect(jsonPath("$.details.start").exists());

        verify(service, never()).findBetween(any(), any());
    }

    @Test
    void shouldReturn409WhenEndDateIsBeforeStartDate() throws Exception {
        when(service.findBetween(any(), any()))
                .thenThrow(new InvalidTreatmentDateException("End date is before start date"));

        mockMvc.perform(get("/api/treatments")
                        .param("start", "2026-08-31T00:00:00")
                        .param("end", "2026-08-01T00:00:00"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("End date is before start date"));
    }
}
