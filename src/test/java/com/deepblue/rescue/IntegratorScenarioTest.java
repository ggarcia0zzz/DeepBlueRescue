package com.deepblue.rescue;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.Expertise;
import com.deepblue.rescue.domain.MedicalRecord;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.ExpertiseRepository;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.repository.RescueCenterRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@Transactional
public class IntegratorScenarioTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    "postgres:18-alpine")
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired
    private RescueCenterRepository rescueCenterRepository;

    @Autowired
    private RescueCaseRepository rescueCaseRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private ExpertiseRepository expertiseRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Test
    void sePersisteElEscenarioCompletoDeLaTortuga() {
        RescueCenter centro = construirEscenario();

        RescueCase caso = rescueCaseRepository.findByCaseCode("RES-2026-100").orElseThrow();
        Animal tortuga = caso.getAnimal();

        assertThat(centro.getId()).isNotNull();
        assertThat(caso.getId()).isNotNull();
        assertThat(tortuga).isNotNull();
        assertThat(tortuga.getId()).isNotNull();
        assertThat(tortuga.getMedicalRecord()).isNotNull();
        assertThat(tortuga.getMedicalRecord().getId()).isNotNull();

        List<Treatment> tratamientos = treatmentRepository.findByAnimal_IdOrderByPerformedAtAsc(tortuga.getId());
        assertThat(tratamientos).hasSize(2);
        assertThat(tratamientos)
                .extracting(Treatment::getDescription)
                .containsExactly("Cleaning of left front flipper", "Subcutaneous fluid therapy");
        assertThat(tratamientos).allSatisfy(t -> assertThat(t.getSpecialist().getProfessionalCode()).isEqualTo("SPEC-001"));
    }

    @Test
    void consulta1_existeElCasoRes2026100() {
        construirEscenario();

        assertThat(rescueCaseRepository.existsByCaseCode("RES-2026-100")).isTrue();
        assertThat(rescueCaseRepository.existsByCaseCode("RES-NO-EXISTE")).isFalse();
    }


    @Test
    void consulta2_casosEnRehabilitacion() {
        construirEscenario();

        List<RescueCase> enRehabilitacion =
                rescueCaseRepository.findAllByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);

        assertThat(enRehabilitacion)
                .extracting(RescueCase::getCaseCode)
                .contains("RES-2026-100");
    }


    @Test
    void consulta3_animalesDelCentroDbCar() {
        construirEscenario();

        List<Animal> animalesDbCar = animalRepository.findByRescueCase_RescueCenter_Code("DB-CAR");

        assertThat(animalesDbCar)
                .extracting(Animal::getAnimalCode)
                .contains("AN-2026-100");
    }


    @Test
    void consulta4_animalesConNombreComunQueContieneTurtle() {
        construirEscenario();

        List<Animal> tortugas = animalRepository.findByCommonNameContainingIgnoreCase("turtle");

        assertThat(tortugas)
                .extracting(Animal::getAnimalCode)
                .contains("AN-2026-100");
    }

    @Test
    void consulta5_especialistasConExperienciaEnTrauma() {
        construirEscenario();

        List<Specialist> especialistasEnTrauma = specialistRepository.findActiveByExpertise("Trauma");

        assertThat(especialistasEnTrauma)
                .extracting(Specialist::getProfessionalCode)
                .contains("SPEC-001");
    }

    @Test
    void consulta6_tratamientosDeLaTortugaEnOrdenCronologico() {
        construirEscenario();

        Animal tortuga = animalRepository.findByAnimalCode("AN-2026-100").orElseThrow();

        List<Treatment> tratamientos = treatmentRepository.findByAnimal_IdOrderByPerformedAtAsc(tortuga.getId());

        assertThat(tratamientos)
                .extracting(Treatment::getDescription)
                .containsExactly("Cleaning of left front flipper", "Subcutaneous fluid therapy");
    }

    @Test
    void consulta7_tratamientosDeEspecialistasConExperienciaEnRehabilitacion() {
        construirEscenario();

        List<Treatment> tratamientos = treatmentRepository.findBySpecialistExpertise("Rehabilitation");

        assertThat(tratamientos)
                .extracting(Treatment::getDescription)
                .contains("Cleaning of left front flipper", "Subcutaneous fluid therapy");
    }

    @Test
    void consulta8_tratamientosEntreDosFechas() {
        construirEscenario();

        List<Treatment> tratamientosEnRango = treatmentRepository.findByPerformedAtBetween(
                LocalDateTime.of(2026, 8, 17, 0, 0),
                LocalDateTime.of(2026, 8, 20, 0, 0));

        assertThat(tratamientosEnRango)
                .extracting(Treatment::getDescription)
                .containsExactly("Cleaning of left front flipper", "Subcutaneous fluid therapy");
    }

    private RescueCenter construirEscenario() {
        RescueCenter centro = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta"));

        RescueCase caso = new RescueCase(
                "RES-2026-100", LocalDate.of(2026, 8, 18), "Bahía Concha", RescueStatus.IN_REHABILITATION);
        centro.addCase(caso);

        Animal tortuga = new Animal("AN-2026-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        caso.assignAnimal(tortuga);

        MedicalRecord expediente = new MedicalRecord(
                new BigDecimal("27.80"),
                "STABLE",
                "Injury caused by fishing net",
                "Possible plastic ingestion");
        tortuga.assignMedicalRecord(expediente);
        
        rescueCaseRepository.save(caso);

        Expertise marineReptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow();
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitacion = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org", true);
        elena.addExpertise(marineReptiles);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitacion);
        specialistRepository.save(elena);

        Treatment tratamiento1 = new Treatment(
                tortuga, elena, LocalDateTime.of(2026, 8, 18, 9, 0),
                TreatmentType.WOUND_CARE, "Cleaning of left front flipper");
        Treatment tratamiento2 = new Treatment(
                tortuga, elena, LocalDateTime.of(2026, 8, 19, 9, 0),
                TreatmentType.HYDRATION, "Subcutaneous fluid therapy");
        treatmentRepository.save(tratamiento1);
        treatmentRepository.save(tratamiento2);

        return centro;
    }
}