# DeepBlue Rescue

## 1. Descripción

Este es un proyecto para practicar Spring Boot, JPA, Flyway y Testcontainers. La idea del sistema es simular una app para un centro de rescate de animales marinos: se guardan los casos de rescate, los animales, su historia clínica, los tratamientos que reciben y los especialistas que los atienden con sus áreas de experiencia.

Usa Spring Boot 4.1.1, Spring Data JPA, Hibernate, PostgreSQL y Flyway para manejar los cambios en la base de datos. Los tests usan Testcontainers para levantar una base de datos real en Docker en vez de usar una en memoria.

---

## 2. Modelo de datos

Estas son las entidades del proyecto:

| Entidad | Tabla | Qué guarda |
|---|---|---|
| `RescueCenter` | `rescue_centers` | El centro de rescate (código, nombre, ciudad) |
| `RescueCase` | `rescue_cases` | El caso de rescate (código, fecha, lugar, estado) |
| `Animal` | `animals` | El animal rescatado (código, nombre, sexo, dispositivo GPS) |
| `MedicalRecord` | `medical_records` | La historia clínica inicial del animal |
| `Specialist` | `specialists` | El especialista que atiende (nombre, email, si está activo) |
| `Expertise` | `expertise` | Las áreas en las que un especialista tiene experiencia (ej. Trauma) |
| `Treatment` | `treatments` | Un tratamiento que se le hizo a un animal |
| `specialist_expertise` | — | Tabla intermedia para la relación N a N entre Specialist y Expertise |

**Enums que se usan:**
- `RescueStatus`: ADMITTED, UNDER_EVALUATION, IN_REHABILITATION, READY_FOR_RELEASE, RELEASED, CLOSED
- `TreatmentType`: WOUND_CARE, HYDRATION, MEDICATION, SURGERY, NUTRITION, PHYSIOTHERAPY, OBSERVATION

---

## 3. Relaciones

RescueCenter (1) ──── (N) RescueCase
RescueCase (1) ──── (1) Animal
Animal (1) ──── (1) MedicalRecord
Animal (1) ──── (N) Treatment
Specialist (1) ──── (N) Treatment
Specialist (N) ──── (N) Expertise


- Un `RescueCenter` puede tener muchos `RescueCase` (OneToMany).
- Cada `RescueCase` tiene un solo `Animal` y cada `Animal` pertenece a un solo caso (OneToOne).
- Cada `Animal` tiene una sola `MedicalRecord` (OneToOne), y si se borra el animal se borra también su historia clínica (cascade + orphanRemoval).
- Un `Animal` puede tener varios `Treatment` (OneToMany).
- Un `Specialist` también puede tener varios `Treatment` (OneToMany).
- Un `Specialist` puede tener varias `Expertise` y una `Expertise` puede tener varios especialistas (ManyToMany), por eso existe la tabla intermedia `specialist_expertise`.

Algo que aprendí haciendo este laboratorio: poner una anotación como `@OneToOne` en Java no es suficiente para que los datos estén bien protegidos. La base de datos real necesita sus propios `CONSTRAINT` (NOT NULL, UNIQUE, REFERENCES, CHECK) puestos en las migraciones de Flyway, porque al final es Postgres el que realmente obliga esas reglas.

---

## 4. Cómo ejecutar el proyecto

### Lo que se necesita antes
- Java 21
- Maven (o el wrapper `mvnw` que ya viene incluido)
- Tener PostgreSQL corriendo, o cambiar las variables de entorno de conexión
- Docker Desktop abierto (para poder correr los tests)

### Configuración de conexión

Por defecto usa estos valores si no se ponen variables de entorno:

```yaml
DB_URL:      jdbc:postgresql://localhost:5432/deepblue
DB_USER:     postgres
DB_PASSWORD: postgres
```

### Para correr la aplicación

```bash
mvnw spring-boot:run
```

Cuando arranca, Flyway aplica las migraciones que falten y Hibernate revisa que las entidades coincidan con la base de datos real (`ddl-auto: validate`).

---

## 5. Cómo correr los tests

```bash
mvnw clean test
```

No hace falta tener Postgres instalado a mano, porque los tests levantan su propia base de datos con Testcontainers. Lo que sí hace falta es tener Docker Desktop abierto, si no, los tests fallan.

Si todo sale bien debería aparecer al final: `BUILD SUCCESS`.

---

## 6. Qué es Flyway y para qué se usó acá

Flyway es la herramienta que se encarga de ir "versionando" los cambios en la base de datos. En vez de crear las tablas a mano, cada cambio se escribe como un archivo `.sql` con un número de versión, y Flyway se encarga de aplicarlos en orden la primera vez que corre la app.

Los archivos de migración de este proyecto están en `src/main/resources/db/migration`:

- **V1__create_schema.sql**: crea todas las tablas con sus constraints (UNIQUE, REFERENCES, CHECK para el status) y algunos índices.
- **V2__insert_expertise_catalog.sql**: inserta los datos iniciales de la tabla `expertise` (Marine Reptiles, Marine Mammals, Marine Birds, Trauma, Rehabilitation, Toxicology).
- **V3__add_tracking_device_to_animal.sql**: agrega la columna `tracking_device_code` a la tabla `animals`, para guardar el código del dispositivo GPS.

Lo importante es que una migración ya aplicada nunca se toca ni se edita — si hay que cambiar algo, se crea una migración nueva (por eso V3 es un archivo aparte y no se metió el cambio dentro de V1).

Como la app usa `ddl-auto: validate`, Hibernate nunca modifica la base de datos por su cuenta, solo revisa que las entidades Java coincidan con lo que Flyway ya creó. Si no coinciden, la aplicación no arranca.

---

## 7. Qué es Testcontainers y para qué se usó acá

Testcontainers sirve para que los tests de integración corran contra una base de datos Postgres real (dentro de un contenedor Docker), en vez de usar una base de datos en memoria tipo H2 que no se comporta exactamente igual que Postgres.

En el proyecto hay una clase `TestcontainersConfiguration` que define un contenedor de Postgres con la anotación `@ServiceConnection`. Cuando se corren los tests:

1. Se levanta automáticamente un contenedor con la imagen `postgres:latest`.
2. Spring Boot conecta la app a ese contenedor sin que uno tenga que configurar nada a mano.
3. Se ejecutan las migraciones de Flyway (V1, V2, V3) sobre esa base de datos limpia.
4. Al terminar los tests, el contenedor se destruye solo.

Con esto los tests prueban contra una base de datos de verdad, no una simulación, así que si algo falla ahí probablemente también fallaría en producción.

---

## 8. Query Methods que se implementaron

| Repositorio | Método | Para qué sirve |
|---|---|---|
| `AnimalRepository` | `findByAnimalCode(String)` | Buscar un animal por su código |
| `AnimalRepository` | `findByCommonNameContainingIgnoreCase(String)` | Buscar animales por parte del nombre común, sin importar mayúsculas |
| `AnimalRepository` | `findByRescueCase_Status(RescueStatus)` | Animales cuyo caso tiene cierto estado |
| `AnimalRepository` | `findByRescueCase_RescueCenter_Code(String)` | Animales que pertenecen a cierto centro de rescate |
| `RescueCaseRepository` | `findByCaseCode(String)` | Buscar un caso por su código |
| `RescueCaseRepository` | `findAllByStatusOrderByRescueDateAsc(RescueStatus)` | Casos con cierto estado, ordenados por fecha |
| `RescueCaseRepository` | `findAllByRescueCenter_Code(String)` | Casos de un centro determinado |
| `RescueCaseRepository` | `findAllByRescueCenter_CodeAndStatus(String, RescueStatus)` | Casos de un centro, filtrados también por estado |
| `RescueCaseRepository` | `findAllByRescueDateAfterOrderByRescueDateDesc(LocalDateTime)` | Casos después de cierta fecha, del más nuevo al más viejo |
| `RescueCenterRepository` | `findByCode(String)` | Buscar un centro por su código |
| `ExpertiseRepository` | `findByNameIgnoreCase(String)` | Buscar una expertise por nombre, sin importar mayúsculas |
| `TreatmentRepository` | `findByAnimal_IdOrderByPerformedAtAsc(Long)` | Tratamientos de un animal, en orden cronológico |

---

## 9. Consultas JPQL que se implementaron

Estas se hicieron con `@Query` porque atraviesan varias relaciones (necesitan JOIN) o porque necesitaban cosas como DISTINCT o comparar sin importar mayúsculas, que con un Query Method quedaría un nombre de método gigante y difícil de leer.

| Repositorio | Método | Qué hace |
|---|---|---|
| `SpecialistRepository` | `findActiveByExpertise(String)` | Especialistas activos que tengan cierta expertise, sin importar mayúsculas, ordenados por nombre |
| `TreatmentRepository` | `findByPerformedAtBetween(LocalDateTime, LocalDateTime)` | Tratamientos hechos entre dos fechas |
| `TreatmentRepository` | `findByAnimalRescueCenterCode(String)` | Tratamientos de animales de un centro específico (recorre Treatment → Animal → RescueCase → RescueCenter) |
| `TreatmentRepository` | `findBySpecialistExpertise(String)` | Tratamientos hechos por especialistas con cierta expertise, sin repetidos (DISTINCT) |
| `AnimalRepository` | `findByRescueStatusAndSpecialistExpertise(RescueStatus, String)` | Animales en cierto estado que hayan recibido al menos un tratamiento de un especialista con cierta expertise, sin animales repetidos. Es la consulta más "larga" del proyecto porque junta dos caminos: Animal → RescueCase.status y Animal → Treatment → Specialist → Expertise |

---

## 10. Resumen del flujo que se siguió en el laboratorio

Modelo del negocio
↓
Modelo relacional
↓
Constraints en PostgreSQL (Flyway)
↓
Entidades JPA
↓
Repositories
↓
Consultas (Query Methods / JPQL)
↓
Tests de integración (Testcontainers)

