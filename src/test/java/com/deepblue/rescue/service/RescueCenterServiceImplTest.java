package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.dto.CreateRescueCenterDto;
import com.deepblue.rescue.dto.RescueCenterDto;
import com.deepblue.rescue.exception.DuplicateResourceException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCenterMapper;
import com.deepblue.rescue.repository.RescueCenterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RescueCenterServiceImplTest {

    @Mock
    private RescueCenterRepository rescueCenterRepository;

    @Mock
    private RescueCenterMapper rescueCenterMapper;

    @InjectMocks
    private RescueCenterServiceImpl service;

    @Test
    void shouldCreateCenterWithNormalizedCode() {
        var request = new CreateRescueCenterDto(" db-car ", " DeepBlue Caribbean ", " Santa Marta ");
        var expected = new RescueCenterDto(1L, "DB-CAR", "DeepBlue Caribbean", "Santa Marta");

        when(rescueCenterRepository.existsByCode("DB-CAR")).thenReturn(false);
        when(rescueCenterRepository.save(any(RescueCenter.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rescueCenterMapper.toDto(any(RescueCenter.class))).thenReturn(expected);

        RescueCenterDto result = service.create(request);

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<RescueCenter> captor = ArgumentCaptor.forClass(RescueCenter.class);
        verify(rescueCenterRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("DB-CAR");
        assertThat(captor.getValue().getName()).isEqualTo("DeepBlue Caribbean");
        assertThat(captor.getValue().getCity()).isEqualTo("Santa Marta");
    }

    @Test
    void shouldRejectDuplicatedCode() {
        var request = new CreateRescueCenterDto("DB-CAR", "DeepBlue Caribbean", "Santa Marta");

        when(rescueCenterRepository.existsByCode("DB-CAR")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(rescueCenterRepository, never()).save(any());
        verifyNoInteractions(rescueCenterMapper);
    }

    @Test
    void shouldThrowWhenCenterDoesNotExist() {
        when(rescueCenterRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(rescueCenterMapper);
    }
}
