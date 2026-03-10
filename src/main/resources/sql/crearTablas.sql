CREATE TABLE IF NOT EXISTS equipo
(
    id_equipo SMALLINT PRIMARY KEY,
    nombre    VARCHAR(20) NOT NULL UNIQUE
);

INSERT INTO equipo (id_equipo, nombre)
VALUES (1, 'NAVAL'),
       (2, 'AEREO')
ON CONFLICT (id_equipo) DO NOTHING;

CREATE TABLE IF NOT EXISTS tipo_unidad
(
    id_tipo_unidad SMALLINT PRIMARY KEY,
    nombre         VARCHAR(30) NOT NULL UNIQUE
);

INSERT INTO tipo_unidad (id_tipo_unidad, nombre)
VALUES (1, 'PORTADRON_NAVAL'),
       (2, 'PORTADRON_AEREO'),
       (3, 'DRON_NAVAL'),
       (4, 'DRON_AEREO')
ON CONFLICT (id_tipo_unidad) DO NOTHING;

CREATE TABLE IF NOT EXISTS partida
(
    id_partida   BIGSERIAL PRIMARY KEY,
    estado       VARCHAR(50) NOT NULL,
    guardada     BOOLEAN     NOT NULL DEFAULT FALSE,
    terminada    BOOLEAN     NOT NULL DEFAULT FALSE,
    codigo_unico VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS jugador
(
    id_jugador BIGSERIAL PRIMARY KEY,
    id_partida BIGINT      NOT NULL,
    id_equipo  SMALLINT    NOT NULL,
    nickname   VARCHAR(50) NOT NULL,

    CONSTRAINT fk_jugador_partida
        FOREIGN KEY (id_partida)
            REFERENCES partida (id_partida)
            ON DELETE CASCADE,

    CONSTRAINT fk_jugador_equipo
        FOREIGN KEY (id_equipo)
            REFERENCES equipo (id_equipo),

    CONSTRAINT uq_jugador_partida_equipo
        UNIQUE (id_partida, id_equipo)
);

CREATE TABLE IF NOT EXISTS unidad
(
    id_unidad      BIGSERIAL PRIMARY KEY,
    id_jugador     BIGINT   NOT NULL,
    id_tipo_unidad SMALLINT NOT NULL,

    coordenada_x   INTEGER  NOT NULL,
    coordenada_y   INTEGER  NOT NULL,
    coordenada_z   INTEGER,

    combustible    INTEGER  NOT NULL,
    municion       INTEGER  NOT NULL,
    destruida      BOOLEAN  NOT NULL DEFAULT FALSE,
    inhabilitada   BOOLEAN  NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_unidad_jugador
        FOREIGN KEY (id_jugador)
            REFERENCES jugador (id_jugador)
            ON DELETE CASCADE,

    CONSTRAINT fk_unidad_tipo_unidad
        FOREIGN KEY (id_tipo_unidad)
            REFERENCES tipo_unidad (id_tipo_unidad)
);

CREATE TABLE IF NOT EXISTS portadron_naval
(
    id_unidad  BIGINT PRIMARY KEY,
    integridad INTEGER NOT NULL,

    CONSTRAINT fk_portadron_naval_unidad
        FOREIGN KEY (id_unidad)
            REFERENCES unidad (id_unidad)
            ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS portadron_aereo
(
    id_unidad  BIGINT PRIMARY KEY,
    integridad INTEGER NOT NULL,

    CONSTRAINT fk_portadron_aereo_unidad
        FOREIGN KEY (id_unidad)
            REFERENCES unidad (id_unidad)
            ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS dron_naval
(
    id_unidad          BIGINT PRIMARY KEY,
    id_portadron_naval BIGINT NOT NULL,

    CONSTRAINT fk_dron_naval_unidad
        FOREIGN KEY (id_unidad)
            REFERENCES unidad (id_unidad)
            ON DELETE CASCADE,

    CONSTRAINT fk_dron_naval_portadron
        FOREIGN KEY (id_portadron_naval)
            REFERENCES portadron_naval (id_unidad)
            ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS dron_aereo
(
    id_unidad          BIGINT PRIMARY KEY,
    id_portadron_aereo BIGINT NOT NULL,

    CONSTRAINT fk_dron_aereo_unidad
        FOREIGN KEY (id_unidad)
            REFERENCES unidad (id_unidad)
            ON DELETE CASCADE,

    CONSTRAINT fk_dron_aereo_portadron
        FOREIGN KEY (id_portadron_aereo)
            REFERENCES portadron_aereo (id_unidad)
            ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS puntaje
(
    id_puntaje     BIGSERIAL PRIMARY KEY,
    id_jugador     BIGINT   NOT NULL UNIQUE,
    id_partida     BIGINT   NOT NULL,
    valor          INTEGER  NOT NULL,
    fecha_registro TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_puntaje_partida
        FOREIGN KEY (id_partida)
            REFERENCES partida (id_partida)
            ON DELETE CASCADE,

    CONSTRAINT fk_puntaje_jugador
        FOREIGN KEY (id_jugador)
            REFERENCES jugador (id_jugador)
            ON DELETE CASCADE,

    CONSTRAINT uq_puntaje_partida_jugador
        UNIQUE (id_partida, id_jugador)
);

CREATE INDEX IF NOT EXISTS idx_puntaje_valor_desc
    ON puntaje (valor DESC);

