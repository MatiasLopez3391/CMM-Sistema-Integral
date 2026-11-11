-- =====================================================================
-- F1 · Núcleo y Afiliados (rango V100-V199)
-- =====================================================================

-- Historial de bloqueos/desbloqueos manuales (H4: la regla automática
-- queda para desarrollo futuro; se registra quién, cuándo y por qué).
CREATE TABLE bloqueo_historial (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    afiliado_id BIGINT       NOT NULL,
    accion      VARCHAR(12)  NOT NULL,
    marca       VARCHAR(80),
    motivo      VARCHAR(500) NOT NULL,
    usuario     VARCHAR(80)  NOT NULL,
    fecha       DATETIME     NOT NULL,
    CONSTRAINT fk_bloqueo_afiliado FOREIGN KEY (afiliado_id) REFERENCES afiliado (id)
);
CREATE INDEX ix_bloqueo_afiliado ON bloqueo_historial (afiliado_id);

-- Historia clínica (entradas cronológicas). Acceso restringido por rol
-- cuando se active el login.
CREATE TABLE historia_clinica_entrada (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    afiliado_id BIGINT        NOT NULL,
    fecha       DATE          NOT NULL,
    tipo        VARCHAR(30)   NOT NULL,
    profesional VARCHAR(120),
    diagnostico VARCHAR(200),
    detalle     VARCHAR(4000) NOT NULL,
    created_at  DATETIME,
    created_by  VARCHAR(80),
    updated_at  DATETIME,
    updated_by  VARCHAR(80),
    CONSTRAINT fk_hc_afiliado FOREIGN KEY (afiliado_id) REFERENCES afiliado (id)
);
CREATE INDEX ix_hc_afiliado ON historia_clinica_entrada (afiliado_id, fecha);
