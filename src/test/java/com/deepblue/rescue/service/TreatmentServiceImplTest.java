package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.RegisterTreatmentDto;
import com.deepblue.rescue.dto.TreatmentDto;
import com.deepblue.rescue.exception.CaseNotUnderCareException;
import com.deepblue.rescue.exception.InactiveSpecialistException;
import com.deepblue.rescue.exception.InvalidTreatmentDateException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class TreatmentServiceImplTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentMapper treatmentMapper;

    private TreatmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TreatmentServiceImpl(
                treatmentRepository, animalRepository, specialistRepository, treatmentMapper, CLOCK);
    }

    private Animal animalInCase(RescueStatus status) {
        RescueCase rescueCase = new RescueCase("RES-1", LocalDate.of(2026, 9, 20), "Playa Blanca", status);
        Animal animal = new Animal("AN-1", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        rescueCase.assignAnimal(animal);
        return animal;
    }

    private Specialist specialist(boolean active) {
        Specialist specialist = new Specialist("SP-1", "Elena", "Vargas", "elena@deepblue.org", active);
        ReflectionTestUtils.setField(specialist, "id", 20L);
        return specialist;
    }

    private RegisterTreatmentDto request(LocalDateTime performedAt) {
        return new RegisterTreatmentDto(10L, 20L, performedAt, TreatmentType.WOUND_CARE, " Cleaned wound ");
    }

    @Test
    void shouldRegisterTreatment() {
        Animal animal = animalInCase(RescueStatus.IN_REHABILITATION);
        Specialist specialist = specialist(true);
        var expected = new TreatmentDto(1L, 10L, "AN-1", 20L, "Elena Vargas",
                LocalDateTime.of(2026, 9, 27, 10, 0), TreatmentType.WOUND_CARE, "Cleaned wound");

        when(animalRepository.findById(10L)).thenReturn(Optional.of(animal));
        when(specialistRepository.findById(20L)).thenReturn(Optional.of(specialist));
        when(treatmentRepository.save(any(Treatment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(treatmentMapper.toDto(any(Treatment.class))).thenReturn(expected);

        TreatmentDto result = service.register(request(LocalDateTime.of(2026, 9, 27, 10, 0)));

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<Treatment> captor = ArgumentCaptor.forClass(Treatment.class);
        verify(treatmentRepository).save(captor.capture());
        assertThat(captor.getValue().getAnimal()).isSameAs(animal);
        assertThat(captor.getValue().getSpecialist()).isSameAs(specialist);
        assertThat(captor.getValue().getDescription()).isEqualTo("Cleaned wound");
    }

    @Test
    void shouldRejectUnknownAnimal() {
        when(animalRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(request(LocalDateTime.of(2026, 9, 27, 10, 0))))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(specialistRepository, treatmentRepository);
    }

    @Test
    void shouldRejectUnknownSpecialist() {
        when(animalRepository.findById(10L)).thenReturn(Optional.of(animalInCase(RescueStatus.ADMITTED)));
        when(specialistRepository.findById(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(request(LocalDateTime.of(2026, 9, 27, 10, 0))))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectInactiveSpecialist() {
        when(animalRepository.findById(10L)).thenReturn(Optional.of(animalInCase(RescueStatus.IN_REHABILITATION)));
        when(specialistRepository.findById(20L)).thenReturn(Optional.of(specialist(false)));

        assertThatThrownBy(() -> service.register(request(LocalDateTime.of(2026, 9, 27, 10, 0))))
                .isInstanceOf(InactiveSpecialistException.class);

        verify(treatmentRepository, never()).save(any());
        verifyNoInteractions(treatmentMapper);
    }

    @Test
    void shouldRejectTreatmentWhenCaseIsReleased() {
        when(animalRepository.findById(10L)).thenReturn(Optional.of(animalInCase(RescueStatus.RELEASED)));
        when(specialistRepository.findById(20L)).thenReturn(Optional.of(specialist(true)));

        assertThatThrownBy(() -> service.register(request(LocalDateTime.of(2026, 9, 27, 10, 0))))
                .isInstanceOf(CaseNotUnderCareException.class);

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectFutureTreatmentDate() {
        when(animalRepository.findById(10L)).thenReturn(Optional.of(animalInCase(RescueStatus.IN_REHABILITATION)));
        when(specialistRepository.findById(20L)).thenReturn(Optional.of(specialist(true)));

        assertThatThrownBy(() -> service.register(request(LocalDateTime.of(2026, 9, 28, 18, 0))))
                .isInstanceOf(InvalidTreatmentDateException.class);

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectTreatmentBeforeRescueDate() {
        when(animalRepository.findById(10L)).thenReturn(Optional.of(animalInCase(RescueStatus.IN_REHABILITATION)));
        when(specialistRepository.findById(20L)).thenReturn(Optional.of(specialist(true)));

        assertThatThrownBy(() -> service.register(request(LocalDateTime.of(2026, 9, 19, 23, 0))))
                .isInstanceOf(InvalidTreatmentDateException.class);

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvertedDateRange() {
        assertThatThrownBy(() -> service.findBetween(
                LocalDateTime.of(2026, 9, 10, 0, 0), LocalDateTime.of(2026, 9, 1, 0, 0)))
                .isInstanceOf(InvalidTreatmentDateException.class);

        verifyNoInteractions(treatmentRepository);
    }
}
