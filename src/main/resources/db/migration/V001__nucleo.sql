-- =====================================================================
-- Núcleo común (Semana 0 / tag v0-base). Entidades compartidas por todos
-- los frentes: no se modifican sin acuerdo del equipo.
-- Compatible con MySQL 8 y con H2 en modo MySQL.
-- =====================================================================

CREATE TABLE numerador (
    clave   VARCHAR(40) NOT NULL PRIMARY KEY,
    ultimo  BIGINT      NOT NULL
);

CREATE TABLE auditoria_cambio (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    entidad    VARCHAR(60)  NOT NULL,
    entidad_id BIGINT       NOT NULL,
    accion     VARCHAR(30)  NOT NULL,
    detalle    VARCHAR(1000),
    usuario    VARCHAR(80)  NOT NULL,
    fecha      DATETIME     NOT NULL
);
CREATE INDEX ix_auditoria_entidad ON auditoria_cambio (entidad, entidad_id);

CREATE TABLE area (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(20)  NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL
);

CREATE TABLE sector (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    area_id BIGINT       NOT NULL,
    codigo  VARCHAR(20)  NOT NULL UNIQUE,
    nombre  VARCHAR(100) NOT NULL,
    CONSTRAINT fk_sector_area FOREIGN KEY (area_id) REFERENCES area (id)
);

-- Plan: código + nombre + nivel de cobertura (H1 del análisis de coherencia).
CREATE TABLE plan (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo               VARCHAR(10)   NOT NULL UNIQUE,
    nombre               VARCHAR(100)  NOT NULL,
    cobertura_porcentaje DECIMAL(5, 2) NOT NULL,
    activo               BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at           DATETIME,
    created_by           VARCHAR(80),
    updated_at           DATETIME,
    updated_by           VARCHAR(80)
);

-- Derivadora: obra social de origen del afiliado (H11).
CREATE TABLE derivadora (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(120) NOT NULL,
    cuit          VARCHAR(13)  UNIQUE,
    convenio      VARCHAR(200),
    vigente_desde DATE,
    vigente_hasta DATE,
    activa        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME,
    created_by    VARCHAR(80),
    updated_at    DATETIME,
    updated_by    VARCHAR(80)
);

CREATE TABLE afiliado (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    nro_afiliado          VARCHAR(12)  NOT NULL UNIQUE,
    titular_id            BIGINT,
    parentesco            VARCHAR(20)  NOT NULL,
    apellido              VARCHAR(80)  NOT NULL,
    nombre                VARCHAR(80)  NOT NULL,
    tipo_documento        VARCHAR(10)  NOT NULL,
    documento             VARCHAR(15)  NOT NULL,
    cuil                  VARCHAR(13),
    fecha_nacimiento      DATE         NOT NULL,
    sexo                  VARCHAR(1)   NOT NULL,
    email                 VARCHAR(120),
    telefono              VARCHAR(40),
    domicilio             VARCHAR(150),
    localidad             VARCHAR(80),
    plan_id               BIGINT       NOT NULL,
    area_id               BIGINT       NOT NULL,
    sector_id             BIGINT       NOT NULL,
    derivadora_id         BIGINT,
    via_cobro             VARCHAR(20),
    carencia              VARCHAR(20)  NOT NULL,
    estado_especial       VARCHAR(80),
    estado_especial_desde DATE,
    estado_especial_hasta DATE,
    estado                VARCHAR(10)  NOT NULL,
    fecha_alta            DATE         NOT NULL,
    fecha_baja            DATE,
    motivo_baja           VARCHAR(150),
    bloqueado             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            DATETIME,
    created_by            VARCHAR(80),
    updated_at            DATETIME,
    updated_by            VARCHAR(80),
    CONSTRAINT fk_afiliado_titular    FOREIGN KEY (titular_id)    REFERENCES afiliado (id),
    CONSTRAINT fk_afiliado_plan       FOREIGN KEY (plan_id)       REFERENCES plan (id),
    CONSTRAINT fk_afiliado_area       FOREIGN KEY (area_id)       REFERENCES area (id),
    CONSTRAINT fk_afiliado_sector     FOREIGN KEY (sector_id)     REFERENCES sector (id),
    CONSTRAINT fk_afiliado_derivadora FOREIGN KEY (derivadora_id) REFERENCES derivadora (id),
    CONSTRAINT uq_afiliado_documento  UNIQUE (tipo_documento, documento)
);
CREATE INDEX ix_afiliado_apellido ON afiliado (apellido, nombre);
CREATE INDEX ix_afiliado_titular ON afiliado (titular_id);
