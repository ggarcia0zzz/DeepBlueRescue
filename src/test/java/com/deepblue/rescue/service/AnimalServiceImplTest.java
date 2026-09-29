package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.dto.AnimalDto;
import com.deepblue.rescue.dto.AssignTrackingDeviceDto;
import com.deepblue.rescue.exception.DuplicateResourceException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private AnimalMapper animalMapper;

    @InjectMocks
    private AnimalServiceImpl service;

    private Animal turtle() {
        return new Animal("AN-1", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
    }

    @Test
    void shouldAssignTrackingDevice() {
        Animal animal = turtle();
        var dto = new AnimalDto(1L, "AN-1", "Green Sea Turtle", "Chelonia mydas",
                AnimalSex.UNKNOWN, "GPS-001", null);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(animal));
        when(animalRepository.existsByTrackingDeviceCode("GPS-001")).thenReturn(false);
        when(animalMapper.toDto(animal)).thenReturn(dto);

        AnimalDto result = service.assignTrackingDevice(1L, new AssignTrackingDeviceDto(" gps-001 "));

        assertThat(result).isEqualTo(dto);
        assertThat(animal.getTrackingDeviceCode()).isEqualTo("GPS-001");
    }

    @Test
    void shouldRejectDeviceAlreadyUsedByAnotherAnimal() {
        Animal animal = turtle();

        when(animalRepository.findById(1L)).thenReturn(Optional.of(animal));
        when(animalRepository.existsByTrackingDeviceCode("GPS-001")).thenReturn(true);

        assertThatThrownBy(() -> service.assignTrackingDevice(1L, new AssignTrackingDeviceDto("GPS-001")))
                .isInstanceOf(DuplicateResourceException.class);

        assertThat(animal.getTrackingDeviceCode()).isNull();
    }

    @Test
    void shouldNotCheckUniquenessWhenDeviceDoesNotChange() {
        Animal animal = turtle();
        animal.assignTrackingDeviceCode("GPS-001");

        when(animalRepository.findById(1L)).thenReturn(Optional.of(animal));
        when(animalMapper.toDto(animal)).thenReturn(null);

        service.assignTrackingDevice(1L, new AssignTrackingDeviceDto("GPS-001"));

        verify(animalRepository, never()).existsByTrackingDeviceCode(any());
    }

    @Test
    void shouldThrowWhenAnimalDoesNotExist() {
        when(animalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignTrackingDevice(99L, new AssignTrackingDeviceDto("GPS-001")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
