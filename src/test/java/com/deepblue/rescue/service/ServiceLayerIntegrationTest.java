package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.AdmitRescueDto;
import com.deepblue.rescue.dto.ChangeRescueStatusDto;
import com.deepblue.rescue.dto.CreateMedicalRecordDto;
import com.deepblue.rescue.dto.CreateRescueCenterDto;
import com.deepblue.rescue.dto.RegisterSpecialistDto;
import com.deepblue.rescue.dto.RegisterTreatmentDto;
import com.deepblue.rescue.dto.RescueCaseDto;
import com.deepblue.rescue.dto.SpecialistDto;
import com.deepblue.rescue.exception.DuplicateResourceException;
import com.deepblue.rescue.exception.ReleaseRequirementsNotMetException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class ServiceLayerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired
    private RescueCenterService centerService;

    @Autowired
    private SpecialistService specialistService;

    @Autowired
    private RescueCaseService rescueCaseService;

    @Autowired
    private TreatmentService treatmentService;

    private RescueCaseDto admitSampleRescue() {
        centerService.create(new CreateRescueCenterDto("db-car", "DeepBlue Caribbean Center", "Santa Marta"));

        return rescueCaseService.admit(new AdmitRescueDto(
                "res-2026-001", LocalDate.now().minusDays(1), "Bahía Concha", "db-car",
                "an-2026-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN, null,
                new CreateMedicalRecordDto(new BigDecimal("28.40"), "stable", "Left flipper injury", null)));
    }

    @Test
    void fullRescueFlow() {
        RescueCaseDto rescue = admitSampleRescue();

        assertThat(rescue.caseCode()).isEqualTo("RES-2026-001");
        assertThat(rescue.status()).isEqualTo(RescueStatus.ADMITTED);
        assertThat(rescue.centerCode()).isEqualTo("DB-CAR");
        assertThat(rescue.animal().animalCode()).isEqualTo("AN-2026-001");
        assertThat(rescue.animal().medicalRecord().initialCondition()).isEqualTo("STABLE");

        SpecialistDto elena = specialistService.register(new RegisterSpecialistDto(
                "sp-001", "Elena", "Vargas", "Elena.Vargas@DeepBlue.org", Set.of("Trauma")));

        assertThat(elena.email()).isEqualTo("elena.vargas@deepblue.org");
        assertThat(elena.expertiseAreas()).containsExactly("Trauma");

        treatmentService.register(new RegisterTreatmentDto(
                rescue.animal().id(), elena.id(), LocalDateTime.now().minusHours(1),
                TreatmentType.WOUND_CARE, "Wound cleaned"));

        assertThat(treatmentService.findByAnimal(rescue.animal().id())).hasSize(1);
        assertThat(treatmentService.findByAnimal(rescue.animal().id()).get(0).specialistName())
                .isEqualTo("Elena Vargas");

        rescueCaseService.changeStatus(rescue.id(), new ChangeRescueStatusDto(RescueStatus.UNDER_EVALUATION));
        rescueCaseService.changeStatus(rescue.id(), new ChangeRescueStatusDto(RescueStatus.IN_REHABILITATION));
        RescueCaseDto ready = rescueCaseService.changeStatus(
                rescue.id(), new ChangeRescueStatusDto(RescueStatus.READY_FOR_RELEASE));

        assertThat(ready.status()).isEqualTo(RescueStatus.READY_FOR_RELEASE);
    }

    @Test
    void cannotBeReadyForReleaseWithoutTreatments() {
        RescueCaseDto rescue = admitSampleRescue();

        rescueCaseService.changeStatus(rescue.id(), new ChangeRescueStatusDto(RescueStatus.UNDER_EVALUATION));
        rescueCaseService.changeStatus(rescue.id(), new ChangeRescueStatusDto(RescueStatus.IN_REHABILITATION));

        assertThatThrownBy(() -> rescueCaseService.changeStatus(
                rescue.id(), new ChangeRescueStatusDto(RescueStatus.READY_FOR_RELEASE)))
                .isInstanceOf(ReleaseRequirementsNotMetException.class);
    }

    @Test
    void cannotAdmitTwoRescuesWithTheSameCode() {
        admitSampleRescue();

        assertThatThrownBy(this::admitSampleRescueAgainSameCodes)
                .isInstanceOf(DuplicateResourceException.class);
    }

    private void admitSampleRescueAgainSameCodes() {
        rescueCaseService.admit(new AdmitRescueDto(
                "RES-2026-001", LocalDate.now().minusDays(1), "Playa Blanca", "DB-CAR",
                "AN-2026-999", "Humpback Whale", "Megaptera novaeangliae", AnimalSex.FEMALE, null,
                new CreateMedicalRecordDto(new BigDecimal("1200.00"), "CRITICAL", null, null)));
    }

    @Test
    void beanValidationRejectsInvalidRequests() {
        assertThatThrownBy(() -> centerService.create(new CreateRescueCenterDto(" ", "Name", "City")))
                .isInstanceOf(ConstraintViolationException.class);
    }
}
