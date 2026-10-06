package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.AnimalDto;
import com.deepblue.rescue.dto.AssignTrackingDeviceDto;
import com.deepblue.rescue.dto.TreatmentDto;
import com.deepblue.rescue.exception.DuplicateResourceException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.AnimalService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnimalController.class)
class AnimalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnimalService animalService;

    @MockitoBean
    private TreatmentService treatmentService;

    private AnimalDto turtle() {
        return new AnimalDto(1L, "AN-2026-001", "Green Sea Turtle", "Chelonia mydas",
                AnimalSex.FEMALE, null, null);
    }

    // ---------- GET /api/animals/{id} ----------

    @Test
    void shouldReturnAnimalById() throws Exception {
        when(animalService.findById(1L)).thenReturn(turtle());

        mockMvc.perform(get("/api/animals/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalCode").value("AN-2026-001"));

        verify(animalService).findById(1L);
    }

    @Test
    void shouldReturn404WhenAnimalIdDoesNotExist() throws Exception {
        when(animalService.findById(999L)).thenThrow(new ResourceNotFoundException("Animal", 999L));

        mockMvc.perform(get("/api/animals/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Animal not found: 999"))
                .andExpect(jsonPath("$.details").isMap());
    }

    // ---------- GET /api/animals/code/{animalCode} ----------

    @Test
    void shouldReturnAnimalByCode() throws Exception {
        when(animalService.findByCode("AN-2026-001")).thenReturn(turtle());

        mockMvc.perform(get("/api/animals/code/{code}", "AN-2026-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commonName").value("Green Sea Turtle"));

        verify(animalService).findByCode("AN-2026-001");
    }

    @Test
    void shouldReturn404WhenAnimalCodeDoesNotExist() throws Exception {
        when(animalService.findByCode("AN-999")).thenThrow(new ResourceNotFoundException("Animal", "AN-999"));

        mockMvc.perform(get("/api/animals/code/{code}", "AN-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Animal not found: AN-999"));
    }

    // ---------- GET /api/animals?commonName= ----------

    @Test
    void shouldSearchAnimalsByCommonName() throws Exception {
        when(animalService.searchByCommonName("turtle")).thenReturn(List.of(turtle()));

        mockMvc.perform(get("/api/animals").param("commonName", "turtle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(animalService).searchByCommonName("turtle");
    }

    // ---------- GET /api/animals?status= ----------

    @Test
    void shouldReturnAnimalsByStatus() throws Exception {
        when(animalService.findByStatus(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(turtle(), turtle()));

        mockMvc.perform(get("/api/animals").param("status", "IN_REHABILITATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(animalService).findByStatus(RescueStatus.IN_REHABILITATION);
    }

    @Test
    void shouldReturn400WhenAnimalStatusParamIsInvalid() throws Exception {
        mockMvc.perform(get("/api/animals").param("status", "FLYING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameter"))
                .andExpect(jsonPath("$.details.status").exists());

        verify(animalService, never()).findByStatus(any());
    }

    // ---------- GET /api/animals?status=&expertise= ----------

    @Test
    void shouldReturnAnimalsByStatusAndExpertise() throws Exception {
        when(animalService.findByStatusAndSpecialistExpertise(RescueStatus.IN_REHABILITATION, "Surgery"))
                .thenReturn(List.of(turtle()));

        mockMvc.perform(get("/api/animals")
                        .param("status", "IN_REHABILITATION")
                        .param("expertise", "Surgery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(animalService).findByStatusAndSpecialistExpertise(RescueStatus.IN_REHABILITATION, "Surgery");
        // Con ambos parámetros debe ganar el endpoint más específico
        verify(animalService, never()).findByStatus(any());
    }

    // ---------- PATCH /api/animals/{id}/tracking-device ----------

    @Test
    void shouldAssignTrackingDevice() throws Exception {
        AnimalDto tracked = new AnimalDto(1L, "AN-2026-001", "Green Sea Turtle", "Chelonia mydas",
                AnimalSex.FEMALE, "GPS-001", null);
        when(animalService.assignTrackingDevice(1L, new AssignTrackingDeviceDto("GPS-001"))).thenReturn(tracked);

        mockMvc.perform(patch("/api/animals/{id}/tracking-device", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "trackingDeviceCode": "GPS-001" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingDeviceCode").value("GPS-001"));

        verify(animalService).assignTrackingDevice(1L, new AssignTrackingDeviceDto("GPS-001"));
    }

    @Test
    void shouldReturn400WhenTrackingDeviceIsBlank() throws Exception {
        mockMvc.perform(patch("/api/animals/{id}/tracking-device", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "trackingDeviceCode": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details.trackingDeviceCode").exists());

        verify(animalService, never()).assignTrackingDevice(anyLong(), any());
    }

    @Test
    void shouldReturn409WhenTrackingDeviceIsAlreadyUsed() throws Exception {
        when(animalService.assignTrackingDevice(anyLong(), any()))
                .thenThrow(new DuplicateResourceException("Animal", "trackingDeviceCode", "GPS-001"));

        mockMvc.perform(patch("/api/animals/{id}/tracking-device", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "trackingDeviceCode": "GPS-001" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    // ---------- GET /api/animals/{id}/treatments ----------

    @Test
    void shouldReturnAnimalTreatments() throws Exception {
        TreatmentDto treatment = new TreatmentDto(100L, 1L, "AN-2026-001", 2L, "Elena Vargas",
                LocalDateTime.of(2026, 8, 21, 9, 30), TreatmentType.WOUND_CARE, "Cleaning of flipper injury");
        when(treatmentService.findByAnimal(1L)).thenReturn(List.of(treatment));

        mockMvc.perform(get("/api/animals/{id}/treatments", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("WOUND_CARE"));

        verify(treatmentService).findByAnimal(1L);
    }

    @Test
    void shouldReturn404WhenAnimalOfTreatmentsDoesNotExist() throws Exception {
        when(treatmentService.findByAnimal(999L)).thenThrow(new ResourceNotFoundException("Animal", 999L));

        mockMvc.perform(get("/api/animals/{id}/treatments", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Animal not found: 999"));
    }

    // ---------- GET /api/animals/{animalCode}/treatment-eligibility (reto) ----------

    @Test
    void shouldReturnTreatmentEligibility() throws Exception {
        when(animalService.canReceiveTreatment("AN-2026-001")).thenReturn(true);

        mockMvc.perform(get("/api/animals/{code}/treatment-eligibility", "AN-2026-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalCode").value("AN-2026-001"))
                .andExpect(jsonPath("$.eligible").value(true));

        verify(animalService).canReceiveTreatment("AN-2026-001");
    }

    @Test
    void shouldReturn404WhenEligibilityAnimalDoesNotExist() throws Exception {
        when(animalService.canReceiveTreatment("AN-999"))
                .thenThrow(new ResourceNotFoundException("Animal", "AN-999"));

        mockMvc.perform(get("/api/animals/{code}/treatment-eligibility", "AN-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Animal not found: AN-999"));
    }
}
