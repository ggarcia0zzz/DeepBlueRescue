package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Expertise;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.dto.RegisterSpecialistDto;
import com.deepblue.rescue.dto.SpecialistDto;
import com.deepblue.rescue.exception.DuplicateResourceException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.SpecialistMapper;
import com.deepblue.rescue.repository.ExpertiseRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecialistServiceImplTest {

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private ExpertiseRepository expertiseRepository;

    @Mock
    private SpecialistMapper specialistMapper;

    @InjectMocks
    private SpecialistServiceImpl service;

    private RegisterSpecialistDto request() {
        return new RegisterSpecialistDto(
                " sp-001 ", " Elena ", " Vargas ", " Elena.Vargas@DeepBlue.org ", Set.of("trauma"));
    }

    @Test
    void shouldRegisterSpecialistNormalizingEmailAndCode() {
        var expected = new SpecialistDto(1L, "SP-001", "Elena", "Vargas",
                "elena.vargas@deepblue.org", true, Set.of("Trauma"));

        when(specialistRepository.existsByEmail("elena.vargas@deepblue.org")).thenReturn(false);
        when(specialistRepository.existsByProfessionalCode("SP-001")).thenReturn(false);
        when(expertiseRepository.findByNameIgnoreCase("trauma"))
                .thenReturn(Optional.of(new Expertise("Trauma")));
        when(specialistRepository.save(any(Specialist.class))).thenAnswer(inv -> inv.getArgument(0));
        when(specialistMapper.toDto(any(Specialist.class))).thenReturn(expected);

        SpecialistDto result = service.register(request());

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<Specialist> captor = ArgumentCaptor.forClass(Specialist.class);
        verify(specialistRepository).save(captor.capture());

        Specialist saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("elena.vargas@deepblue.org");
        assertThat(saved.getProfessionalCode()).isEqualTo("SP-001");
        assertThat(saved.getFirstName()).isEqualTo("Elena");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getExpertiseAreas()).hasSize(1);
    }

    @Test
    void shouldRejectDuplicatedEmail() {
        when(specialistRepository.existsByEmail("elena.vargas@deepblue.org")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(specialistRepository, never()).save(any());
        verifyNoInteractions(expertiseRepository);
    }

    @Test
    void shouldRejectDuplicatedProfessionalCode() {
        when(specialistRepository.existsByEmail("elena.vargas@deepblue.org")).thenReturn(false);
        when(specialistRepository.existsByProfessionalCode("SP-001")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(specialistRepository, never()).save(any());
    }

    @Test
    void shouldRejectExpertiseThatIsNotInTheCatalog() {
        when(specialistRepository.existsByEmail("elena.vargas@deepblue.org")).thenReturn(false);
        when(specialistRepository.existsByProfessionalCode("SP-001")).thenReturn(false);
        when(expertiseRepository.findByNameIgnoreCase("trauma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(request()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(specialistRepository, never()).save(any());
    }

    @Test
    void shouldDeactivateAndReactivateSpecialist() {
        Specialist specialist = new Specialist("SP-001", "Elena", "Vargas", "elena@deepblue.org", true);

        when(specialistRepository.findById(1L)).thenReturn(Optional.of(specialist));
        when(specialistMapper.toDto(specialist)).thenReturn(null);

        service.deactivate(1L);
        assertThat(specialist.isActive()).isFalse();

        service.activate(1L);
        assertThat(specialist.isActive()).isTrue();
    }

    @Test
    void shouldThrowWhenDeactivatingUnknownSpecialist() {
        when(specialistRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(specialistMapper);
    }
}
