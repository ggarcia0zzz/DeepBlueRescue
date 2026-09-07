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
import org.springframework.jdbc.core.JdbcTemplate;
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
class PersistenceIntegrationTest {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayEjecutoLasMigracionesV1YV2() {
        List<String> versionesAplicadas = jdbcTemplate.queryForList(
                """
                select version
                from flyway_schema_history
                where success = true
                order by installed_rank
                """,
                String.class);

        assertThat(versionesAplicadas).contains("1", "2");
    }

    @Test
    void metodosHeredadosDeJpaRepository() {
        RescueCenter caribe = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");

        RescueCenter guardado = rescueCenterRepository.save(caribe);

        assertThat(guardado.getId()).isNotNull();
        assertThat(rescueCenterRepository.findById(guardado.getId())).isPresent();
        assertThat(rescueCenterRepository.existsById(guardado.getId())).isTrue();
        assertThat(rescueCenterRepository.count()).isEqualTo(1);
    }

    @Test
    void unCentroTieneVariosCasos() {
        RescueCenter caribe = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta"));

        RescueCase caso1 = new RescueCase("RES-2026-101", LocalDate.now(), "Bahía Concha", RescueStatus.ADMITTED);
        RescueCase caso2 = new RescueCase("RES-2026-102", LocalDate.now(), "Playa Blanca", RescueStatus.ADMITTED);
        caribe.addCase(caso1);
        caribe.addCase(caso2);
        rescueCaseRepository.save(caso1);
        rescueCaseRepository.save(caso2);

        List<RescueCase> casosDelCentro = rescueCaseRepository.findAllByRescueCenter_Code("DB-CAR");

        assertThat(casosDelCentro).hasSize(2);
        assertThat(casosDelCentro)
                .allSatisfy(caso -> assertThat(caso.getRescueCenter().getId()).isEqualTo(caribe.getId()));
    }

   
    @Test
    void unCasoTieneUnAnimalAsociado() {
        RescueCenter caribe = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta"));

        RescueCase caso = new RescueCase("RES-2026-001", LocalDate.now(), "Santa Marta", RescueStatus.ADMITTED);
        caribe.addCase(caso);

        Animal tortuga = new Animal("AN-2026-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        caso.assignAnimal(tortuga);

        rescueCaseRepository.save(caso);

        RescueCase casoRecuperado = rescueCaseRepository.findByCaseCode("RES-2026-001").orElseThrow();

        assertThat(casoRecuperado.getAnimal()).isNotNull();
        assertThat(casoRecuperado.getAnimal().getAnimalCode()).isEqualTo("AN-2026-001");
        assertThat(casoRecuperado.getAnimal().getRescueCase().getCaseCode()).isEqualTo("RES-2026-001");
    }

    @Test
    void unAnimalTieneUnaHistoriaClinica() {
        RescueCenter caribe = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta"));

        RescueCase caso = new RescueCase("RES-2026-002", LocalDate.now(), "Santa Marta", RescueStatus.ADMITTED);
        caribe.addCase(caso);

        Animal tortuga = new Animal("AN-2026-002", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        caso.assignAnimal(tortuga);

        MedicalRecord historia = new MedicalRecord(
                new BigDecimal("28.40"),
                "STABLE",
                "Left front flipper injury",
                null);
        tortuga.assignMedicalRecord(historia);

        // Un único save en la raíz del grafo (RescueCase) basta:
        // el cascade ALL se propaga RescueCase -> Animal -> MedicalRecord
        rescueCaseRepository.save(caso);

        assertThat(tortuga.getId()).isNotNull();
        assertThat(historia.getId()).isNotNull();
    }

   
    @Test
    void unEspecialistaTieneVariasAreasDeExperticia() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitacion = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist("SP-001", "Elena", "Vargas", "elena.vargas@deepblue.org", true);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitacion);

        specialistRepository.save(elena);

        Specialist elenaRecuperada = specialistRepository.findById(elena.getId()).orElseThrow();

        assertThat(elenaRecuperada.getExpertiseAreas()).hasSize(2);
        assertThat(elenaRecuperada.getExpertiseAreas())
                .extracting(Expertise::getName)
                .containsExactlyInAnyOrder("Trauma", "Rehabilitation");
    }

   
    @Test
    void queryMethodDeCasosPorEstado() {
        RescueCenter caribe = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta"));

        RescueCase res1 = new RescueCase("RES-001", LocalDate.now(), "Santa Marta", RescueStatus.IN_REHABILITATION);
        RescueCase res2 = new RescueCase("RES-002", LocalDate.now(), "Santa Marta", RescueStatus.READY_FOR_RELEASE);
        RescueCase res3 = new RescueCase("RES-003", LocalDate.now(), "Santa Marta", RescueStatus.IN_REHABILITATION);
        caribe.addCase(res1);
        caribe.addCase(res2);
        caribe.addCase(res3);
        rescueCaseRepository.save(res1);
        rescueCaseRepository.save(res2);
        rescueCaseRepository.save(res3);

        List<RescueCase> enRehabilitacion =
                rescueCaseRepository.findAllByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);

        assertThat(enRehabilitacion).hasSize(2);
        assertThat(enRehabilitacion)
                .extracting(RescueCase::getCaseCode)
                .containsExactlyInAnyOrder("RES-001", "RES-003");
    }

    @Test
    void queryMethodDeAnimalesPorCentroDeRescate() {
        RescueCenter centroCaribe = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta"));
        RescueCenter centroPacifico = rescueCenterRepository.save(
                new RescueCenter("DB-PAC", "DeepBlue Pacific Center", "Buenaventura"));

        RescueCase casoCaribe = new RescueCase("RES-2026-201", LocalDate.now(), "Santa Marta", RescueStatus.ADMITTED);
        centroCaribe.addCase(casoCaribe);
        Animal tortuga = new Animal("AN-2026-201", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        casoCaribe.assignAnimal(tortuga);
        rescueCaseRepository.save(casoCaribe);

        RescueCase casoPacifico = new RescueCase("RES-2026-202", LocalDate.now(), "Buenaventura", RescueStatus.ADMITTED);
        centroPacifico.addCase(casoPacifico);
        Animal ballena = new Animal("AN-2026-202", "Humpback Whale", "Megaptera novaeangliae", AnimalSex.UNKNOWN);
        casoPacifico.assignAnimal(ballena);
        rescueCaseRepository.save(casoPacifico);

        List<Animal> animalesDelCaribe = animalRepository.findByRescueCase_RescueCenter_Code("DB-CAR");

        assertThat(animalesDelCaribe)
                .extracting(Animal::getAnimalCode)
                .containsExactly("AN-2026-201");
    }

    @Test
    void jpqlDeEspecialistasPorExperticia() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitacion = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();
        Expertise mamiferosMarinos = expertiseRepository.findByNameIgnoreCase("Marine Mammals").orElseThrow();
        Expertise avesMarinas = expertiseRepository.findByNameIgnoreCase("Marine Birds").orElseThrow();

        Specialist elena = new Specialist("SP-001", "Elena", "Vargas", "elena.vargas@deepblue.org", true);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitacion);

        Specialist mateo = new Specialist("SP-002", "Mateo", "Restrepo", "mateo.restrepo@deepblue.org", true);
        mateo.addExpertise(mamiferosMarinos);
        mateo.addExpertise(rehabilitacion);

        Specialist sofia = new Specialist("SP-003", "Sofia", "Duarte", "sofia.duarte@deepblue.org", true);
        sofia.addExpertise(avesMarinas);
        sofia.addExpertise(trauma);

        specialistRepository.save(elena);
        specialistRepository.save(mateo);
        specialistRepository.save(sofia);

        List<Specialist> especialistasEnTrauma = specialistRepository.findActiveByExpertise("Trauma");

        assertThat(especialistasEnTrauma)
                .extracting(Specialist::getFirstName)
                .containsExactly("Elena", "Sofia");
    }

    @Test
    void seRegistranVariosTratamientosParaUnAnimal() {
        Animal tortuga = crearAnimalConCaso("RES-2026-301", "AN-2026-301");
        Specialist elena = crearEspecialista("SP-101", "Elena", "Vargas", "Trauma");
        Specialist mateo = crearEspecialista("SP-102", "Mateo", "Restrepo", "Marine Mammals");

        Treatment tratamiento1 = new Treatment(
                tortuga, elena, LocalDateTime.of(2026, 8, 1, 9, 0), TreatmentType.WOUND_CARE, "Treatment 1");
        Treatment tratamiento2 = new Treatment(
                tortuga, elena, LocalDateTime.of(2026, 8, 2, 9, 0), TreatmentType.HYDRATION, "Treatment 2");
        Treatment tratamiento3 = new Treatment(
                tortuga, mateo, LocalDateTime.of(2026, 8, 3, 9, 0), TreatmentType.OBSERVATION, "Treatment 3");

        treatmentRepository.save(tratamiento1);
        treatmentRepository.save(tratamiento2);
        treatmentRepository.save(tratamiento3);

        assertThat(tratamiento1.getId()).isNotNull();
        assertThat(tratamiento2.getId()).isNotNull();
        assertThat(tratamiento3.getId()).isNotNull();
        assertThat(treatmentRepository.count()).isEqualTo(3);
    }

    @Test
    void queryMethodDeTratamientosOrdenadosCronologicamente() {
        Animal tortuga = crearAnimalConCaso("RES-2026-302", "AN-2026-302");
        Specialist elena = crearEspecialista("SP-103", "Elena", "Vargas", "Trauma");
        Specialist mateo = crearEspecialista("SP-104", "Mateo", "Restrepo", "Marine Mammals");

        treatmentRepository.save(new Treatment(
                tortuga, elena, LocalDateTime.of(2026, 8, 1, 9, 0), TreatmentType.WOUND_CARE, "Treatment 1"));
        treatmentRepository.save(new Treatment(
                tortuga, elena, LocalDateTime.of(2026, 8, 2, 9, 0), TreatmentType.HYDRATION, "Treatment 2"));
        treatmentRepository.save(new Treatment(
                tortuga, mateo, LocalDateTime.of(2026, 8, 3, 9, 0), TreatmentType.OBSERVATION, "Treatment 3"));

        List<Treatment> tratamientos = treatmentRepository.findByAnimal_IdOrderByPerformedAtAsc(tortuga.getId());

        assertThat(tratamientos)
                .extracting(Treatment::getDescription)
                .containsExactly("Treatment 1", "Treatment 2", "Treatment 3");
    }

    @Test
    void jpqlDeTratamientosPorIntervaloDeFechas() {
        Animal tortuga = crearAnimalConCaso("RES-2026-303", "AN-2026-303");
        Specialist elena = crearEspecialista("SP-105", "Elena", "Vargas", "Trauma");

        treatmentRepository.save(new Treatment(
                tortuga, elena, LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE, "Treatment 01-08"));
        treatmentRepository.save(new Treatment(
                tortuga, elena, LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.HYDRATION, "Treatment 10-08"));
        treatmentRepository.save(new Treatment(
                tortuga, elena, LocalDateTime.of(2026, 8, 20, 10, 0), TreatmentType.OBSERVATION, "Treatment 20-08"));

        List<Treatment> tratamientosEnRango = treatmentRepository.findByPerformedAtBetween(
                LocalDateTime.of(2026, 8, 5, 0, 0),
                LocalDateTime.of(2026, 8, 15, 0, 0));

        assertThat(tratamientosEnRango).hasSize(1);
        assertThat(tratamientosEnRango.get(0).getDescription()).isEqualTo("Treatment 10-08");
    }

    // ---------------------------------------------------------------
    // Helpers de construcción de datos, comunes a los pasos 56-58
    // ---------------------------------------------------------------
    private Animal crearAnimalConCaso(String caseCode, String animalCode) {
        RescueCenter centro = rescueCenterRepository.save(
                new RescueCenter("DB-CAR-" + caseCode, "DeepBlue Caribbean Center", "Santa Marta"));

        RescueCase caso = new RescueCase(caseCode, LocalDate.now(), "Santa Marta", RescueStatus.IN_REHABILITATION);
        centro.addCase(caso);

        Animal animal = new Animal(animalCode, "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        caso.assignAnimal(animal);

        rescueCaseRepository.save(caso);
        return animal;
    }

    private Specialist crearEspecialista(String professionalCode, String firstName, String lastName, String expertiseName) {
        Expertise expertise = expertiseRepository.findByNameIgnoreCase(expertiseName).orElseThrow();

        Specialist specialist = new Specialist(
                professionalCode, firstName, lastName, professionalCode.toLowerCase() + "@deepblue.org", true);
        specialist.addExpertise(expertise);

        return specialistRepository.save(specialist);
    }
}