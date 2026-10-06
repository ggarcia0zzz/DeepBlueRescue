package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.AdmitRescueDto;
import com.deepblue.rescue.dto.ChangeRescueStatusDto;
import com.deepblue.rescue.dto.CreateMedicalRecordDto;
import com.deepblue.rescue.dto.RescueCaseDto;
import com.deepblue.rescue.exception.DuplicateResourceException;
import com.deepblue.rescue.exception.InvalidRescueDateException;
import com.deepblue.rescue.exception.InvalidStatusTransitionException;
import com.deepblue.rescue.exception.ReleaseRequirementsNotMetException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.repository.RescueCenterRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.RescueCaseServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RescueCaseServiceImplTest {

    // "Hoy" congelado: 2026-09-28
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private RescueCaseRepository rescueCaseRepository;

    @Mock
    private RescueCenterRepository rescueCenterRepository;

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private RescueCaseMapper rescueCaseMapper;

    private RescueCaseServiceImpl service;

    @BeforeEach
    void setUp() {
        // Se construye a mano porque necesita el Clock fijo (mismo orden que los campos de la clase)
        service = new RescueCaseServiceImpl(
                rescueCaseRepository, rescueCenterRepository, animalRepository,
                treatmentRepository, rescueCaseMapper, CLOCK);
    }

    // ---------------------------------------------------------------- admit()

    private AdmitRescueDto admitRequest(LocalDate rescueDate, String trackingDevice) {
        return new AdmitRescueDto(
                " res-2026-001 ", rescueDate, " Bahía Concha ", "db-car",
                "an-2026-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN,
                trackingDevice,
                new CreateMedicalRecordDto(new BigDecimal("28.40"), "stable", "Left flipper injury", " "));
    }

    @Test
    void shouldAdmitRescueBuildingTheWholeGraph() {
        var request = admitRequest(LocalDate.of(2026, 9, 27), " gps-001 ");
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta");
        var expected = new RescueCaseDto(1L, "RES-2026-001", LocalDate.of(2026, 9, 27),
                "Bahía Concha", RescueStatus.ADMITTED, "DB-CAR", "DeepBlue Caribbean", null);

        when(rescueCaseRepository.existsByCaseCode("RES-2026-001")).thenReturn(false);
        when(animalRepository.existsByAnimalCode("AN-2026-001")).thenReturn(false);
        when(animalRepository.existsByTrackingDeviceCode("GPS-001")).thenReturn(false);
        when(rescueCenterRepository.findByCode("DB-CAR")).thenReturn(Optional.of(center));
        when(rescueCaseRepository.save(any(RescueCase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rescueCaseMapper.toDto(any(RescueCase.class))).thenReturn(expected);

        RescueCaseDto result = service.admit(request);

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<RescueCase> captor = ArgumentCaptor.forClass(RescueCase.class);
        verify(rescueCaseRepository).save(captor.capture());

        RescueCase saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(RescueStatus.ADMITTED);
        assertThat(saved.getCaseCode()).isEqualTo("RES-2026-001");
        assertThat(saved.getRescueLocation()).isEqualTo("Bahía Concha");
        assertThat(saved.getRescueCenter()).isSameAs(center);
        assertThat(saved.getAnimal().getAnimalCode()).isEqualTo("AN-2026-001");
        assertThat(saved.getAnimal().getTrackingDeviceCode()).isEqualTo("GPS-001");
        assertThat(saved.getAnimal().getMedicalRecord().getInitialCondition()).isEqualTo("STABLE");
        assertThat(saved.getAnimal().getMedicalRecord().getObservations()).isNull();
    }

    @Test
    void shouldRejectFutureRescueDate() {
        var request = admitRequest(LocalDate.of(2026, 9, 29), null);

        assertThatThrownBy(() -> service.admit(request))
                .isInstanceOf(InvalidRescueDateException.class);

        verifyNoInteractions(rescueCaseRepository, rescueCenterRepository, animalRepository);
    }

    @Test
    void shouldRejectDuplicatedCaseCode() {
        var request = admitRequest(LocalDate.of(2026, 9, 27), null);

        when(rescueCaseRepository.existsByCaseCode("RES-2026-001")).thenReturn(true);

        assertThatThrownBy(() -> service.admit(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(rescueCaseRepository, never()).save(any());
        verifyNoInteractions(rescueCenterRepository);
    }

    @Test
    void shouldRejectDuplicatedAnimalCode() {
        var request = admitRequest(LocalDate.of(2026, 9, 27), null);

        when(rescueCaseRepository.existsByCaseCode("RES-2026-001")).thenReturn(false);
        when(animalRepository.existsByAnimalCode("AN-2026-001")).thenReturn(true);

        assertThatThrownBy(() -> service.admit(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(rescueCaseRepository, never()).save(any());
    }

    @Test
    void shouldRejectTrackingDeviceAlreadyInUse() {
        var request = admitRequest(LocalDate.of(2026, 9, 27), "GPS-001");

        when(rescueCaseRepository.existsByCaseCode("RES-2026-001")).thenReturn(false);
        when(animalRepository.existsByAnimalCode("AN-2026-001")).thenReturn(false);
        when(animalRepository.existsByTrackingDeviceCode("GPS-001")).thenReturn(true);

        assertThatThrownBy(() -> service.admit(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(rescueCaseRepository, never()).save(any());
    }

    @Test
    void shouldRejectUnknownCenter() {
        var request = admitRequest(LocalDate.of(2026, 9, 27), null);

        when(rescueCaseRepository.existsByCaseCode("RES-2026-001")).thenReturn(false);
        when(animalRepository.existsByAnimalCode("AN-2026-001")).thenReturn(false);
        when(rescueCenterRepository.findByCode("DB-CAR")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.admit(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(rescueCaseRepository, never()).save(any());
    }

    // ---------------------------------------------------------- changeStatus()

    private RescueCase caseWithAnimal(RescueStatus status) {
        RescueCase rescueCase = new RescueCase("RES-1", LocalDate.of(2026, 9, 1), "Playa Blanca", status);
        Animal animal = new Animal("AN-1", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        ReflectionTestUtils.setField(animal, "id", 10L);
        rescueCase.assignAnimal(animal);
        return rescueCase;
    }

    @Test
    void shouldAdvanceToTheNextStatus() {
        RescueCase rescueCase = caseWithAnimal(RescueStatus.ADMITTED);

        when(rescueCaseRepository.findById(1L)).thenReturn(Optional.of(rescueCase));
        when(rescueCaseMapper.toDto(rescueCase)).thenReturn(null);

        service.changeStatus(1L, new ChangeRescueStatusDto(RescueStatus.UNDER_EVALUATION));

        assertThat(rescueCase.getStatus()).isEqualTo(RescueStatus.UNDER_EVALUATION);
        verifyNoInteractions(treatmentRepository);
    }

    @Test
    void shouldRejectSkippingStatuses() {
        RescueCase rescueCase = caseWithAnimal(RescueStatus.ADMITTED);

        when(rescueCaseRepository.findById(1L)).thenReturn(Optional.of(rescueCase));

        assertThatThrownBy(() ->
                service.changeStatus(1L, new ChangeRescueStatusDto(RescueStatus.RELEASED)))
                .isInstanceOf(InvalidStatusTransitionException.class);

        assertThat(rescueCase.getStatus()).isEqualTo(RescueStatus.ADMITTED);
    }

    @Test
    void shouldRejectReadyForReleaseWithoutTreatments() {
        RescueCase rescueCase = caseWithAnimal(RescueStatus.IN_REHABILITATION);

        when(rescueCaseRepository.findById(1L)).thenReturn(Optional.of(rescueCase));
        when(treatmentRepository.existsByAnimal_Id(10L)).thenReturn(false);

        assertThatThrownBy(() ->
                service.changeStatus(1L, new ChangeRescueStatusDto(RescueStatus.READY_FOR_RELEASE)))
                .isInstanceOf(ReleaseRequirementsNotMetException.class);

        assertThat(rescueCase.getStatus()).isEqualTo(RescueStatus.IN_REHABILITATION);
        verifyNoInteractions(rescueCaseMapper);
    }

    @Test
    void shouldAllowReadyForReleaseWhenAnimalHasTreatments() {
        RescueCase rescueCase = caseWithAnimal(RescueStatus.IN_REHABILITATION);

        when(rescueCaseRepository.findById(1L)).thenReturn(Optional.of(rescueCase));
        when(treatmentRepository.existsByAnimal_Id(10L)).thenReturn(true);
        when(rescueCaseMapper.toDto(rescueCase)).thenReturn(null);

        service.changeStatus(1L, new ChangeRescueStatusDto(RescueStatus.READY_FOR_RELEASE));

        assertThat(rescueCase.getStatus()).isEqualTo(RescueStatus.READY_FOR_RELEASE);
    }

    @Test
    void shouldThrowWhenCaseDoesNotExist() {
        when(rescueCaseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.changeStatus(99L, new ChangeRescueStatusDto(RescueStatus.UNDER_EVALUATION)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldRejectFindByCenterWhenCenterDoesNotExist() {
        when(rescueCenterRepository.existsByCode("DB-XXX")).thenReturn(false);

        assertThatThrownBy(() -> service.findByCenter("db-xxx"))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(rescueCaseRepository);
    }
}
