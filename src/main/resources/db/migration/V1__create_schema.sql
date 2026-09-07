CREATE TABLE rescue_centers (
                                id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                code VARCHAR(50)  NOT NULL UNIQUE,
                                name VARCHAR(150) NOT NULL,
                                city VARCHAR(100) NOT NULL
);

CREATE TABLE rescue_cases (
                              id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              case_code VARCHAR(50) NOT NULL UNIQUE,
                              rescue_date DATE NOT NULL,
                              rescue_location VARCHAR(150) NOT NULL,
                              status VARCHAR(30) NOT NULL,
                              rescue_center_id BIGINT NOT NULL REFERENCES rescue_centers(id),
                              CONSTRAINT chk_rescue_cases_status CHECK (
                                  status IN ('ADMITTED', 'UNDER_EVALUATION', 'IN_REHABILITATION',
                                             'READY_FOR_RELEASE', 'RELEASED', 'CLOSED')
                                  )
);

CREATE TABLE animals (
                         id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         animal_code VARCHAR(50)  NOT NULL UNIQUE,
                         common_name VARCHAR(100) NOT NULL,
                         scientific_name VARCHAR(150) NOT NULL,
                         sex VARCHAR(20)  NOT NULL,
                         rescue_case_id BIGINT NOT NULL UNIQUE REFERENCES rescue_cases(id)
);

CREATE TABLE medical_records (
                                 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                 animal_id BIGINT NOT NULL UNIQUE REFERENCES animals(id),
                                 initial_weight DECIMAL(6,2) NOT NULL,
                                 initial_condition VARCHAR(30) NOT NULL,
                                 injuries TEXT,
                                 observations TEXT
);

CREATE TABLE specialists (
                             id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             professional_code VARCHAR(50) NOT NULL UNIQUE,
                             first_name VARCHAR(100) NOT NULL,
                             last_name VARCHAR(100) NOT NULL,
                             email VARCHAR(150) NOT NULL UNIQUE,
                             active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE expertise (
                           id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE specialist_expertise (
                                      specialist_id BIGINT NOT NULL REFERENCES specialists(id),
                                      expertise_id BIGINT NOT NULL REFERENCES expertise(id),
                                      PRIMARY KEY (specialist_id, expertise_id)
);

CREATE TABLE treatments (
                            id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            animal_id BIGINT NOT NULL REFERENCES animals(id),
                            specialist_id BIGINT NOT NULL REFERENCES specialists(id),
                            performed_at TIMESTAMP NOT NULL,
                            type VARCHAR(30) NOT NULL,
                            description TEXT
);

CREATE INDEX idx_rescue_cases_rescue_center_id ON rescue_cases(rescue_center_id);
CREATE INDEX idx_rescue_cases_status           ON rescue_cases(status);
CREATE INDEX idx_rescue_cases_rescue_date      ON rescue_cases(rescue_date);
CREATE INDEX idx_treatments_animal_id          ON treatments(animal_id);
CREATE INDEX idx_treatments_specialist_id      ON treatments(specialist_id);
CREATE INDEX idx_treatments_performed_at       ON treatments(performed_at);