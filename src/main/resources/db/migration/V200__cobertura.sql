-- =====================================================================
-- F2 · Cobertura y Prestadores (rango V200-V299)
-- =====================================================================

-- Excepciones de cobertura por rubro (ej. plan "80/100 MED 40%":
-- 80% general, 40% en medicamentos).
CREATE TABLE cobertura_rubro (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id    BIGINT        NOT NULL,
    rubro      VARCHAR(30)   NOT NULL,
    porcentaje DECIMAL(5, 2) NOT NULL,
    CONSTRAINT fk_cobrubro_plan FOREIGN KEY (plan_id) REFERENCES plan (id),
    CONSTRAINT uq_cobrubro UNIQUE (plan_id, rubro)
);

CREATE TABLE prestador (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre     VARCHAR(120) NOT NULL,
    cuit       VARCHAR(13)  UNIQUE,
    tipo       VARCHAR(30)  NOT NULL,
    email      VARCHAR(120),
    activo     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME,
    created_by VARCHAR(80),
    updated_at DATETIME,
    updated_by VARCHAR(80)
);

CREATE TABLE practica (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_cmm         VARCHAR(12)  NOT NULL UNIQUE,
    codigo_nomenclador VARCHAR(12),
    descripcion        VARCHAR(200) NOT NULL,
    rubro              VARCHAR(30)  NOT NULL,
    requiere_auditoria BOOLEAN      NOT NULL DEFAULT FALSE
);

-- Valor convenido con el prestador por práctica, con vigencia (H12).
-- El coseguro NO se guarda: se calcula según la cobertura del plan.
CREATE TABLE valor_convenido (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    prestador_id  BIGINT         NOT NULL,
    practica_id   BIGINT         NOT NULL,
    vigente_desde DATE           NOT NULL,
    importe       DECIMAL(14, 2) NOT NULL,
    origen        VARCHAR(120),
    created_at    DATETIME,
    created_by    VARCHAR(80),
    updated_at    DATETIME,
    updated_by    VARCHAR(80),
    CONSTRAINT fk_valor_prestador FOREIGN KEY (prestador_id) REFERENCES prestador (id),
    CONSTRAINT fk_valor_practica  FOREIGN KEY (practica_id)  REFERENCES practica (id),
    CONSTRAINT uq_valor_vigencia UNIQUE (prestador_id, practica_id, vigente_desde)
);
