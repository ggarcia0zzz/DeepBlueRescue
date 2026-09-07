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
}