-- =====================================================================
-- F4 · Cobranza (rango V400-V499)
-- Importación de .txt de entes de cobro + pagos manuales en ventanilla (H3).
-- =====================================================================

CREATE TABLE medio_cobro (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo             VARCHAR(20)  NOT NULL UNIQUE,
    nombre             VARCHAR(80)  NOT NULL,
    -- Código de cuenta contable (contrapartida del asiento de cobro en FEBBO)
    cuenta_contable    VARCHAR(20)  NOT NULL,
    admite_archivo     BOOLEAN      NOT NULL DEFAULT FALSE,
    activo             BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Diseño de archivo configurable: cuando el CMM entregue el archivo real de
-- cada ente se ajustan posiciones sin tocar código.
CREATE TABLE layout_archivo (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    medio_cobro_id         BIGINT       NOT NULL,
    nombre                 VARCHAR(80)  NOT NULL,
    formato                VARCHAR(15)  NOT NULL,
    delimitador            VARCHAR(3),
    prefijo_registro       VARCHAR(10),
    lineas_encabezado      INT          NOT NULL DEFAULT 0,
    afiliado_pos           INT          NOT NULL,
    afiliado_largo         INT,
    fecha_pos              INT          NOT NULL,
    fecha_largo            INT,
    fecha_formato          VARCHAR(20)  NOT NULL,
    importe_pos            INT          NOT NULL,
    importe_largo          INT,
    importe_decimales_impl INT          NOT NULL DEFAULT 0,
    referencia_pos         INT,
    referencia_largo       INT,
    provisorio             BOOLEAN      NOT NULL DEFAULT TRUE,
    observaciones          VARCHAR(300),
    activo                 BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_layout_medio FOREIGN KEY (medio_cobro_id) REFERENCES medio_cobro (id)
);

CREATE TABLE lote_cobro (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    medio_cobro_id      BIGINT         NOT NULL,
    layout_id           BIGINT         NOT NULL,
    nombre_archivo      VARCHAR(200)   NOT NULL,
    hash_archivo        VARCHAR(64)    NOT NULL,
    estado              VARCHAR(20)    NOT NULL,
    cantidad_registros  INT            NOT NULL,
    cantidad_ok         INT            NOT NULL,
    cantidad_rechazados INT            NOT NULL,
    importe_ok          DECIMAL(14, 2) NOT NULL,
    fecha_confirmacion  DATETIME,
    created_at          DATETIME,
    created_by          VARCHAR(80),
    updated_at          DATETIME,
    updated_by          VARCHAR(80),
    CONSTRAINT fk_lote_medio  FOREIGN KEY (medio_cobro_id) REFERENCES medio_cobro (id),
    CONSTRAINT fk_lote_layout FOREIGN KEY (layout_id)      REFERENCES layout_archivo (id)
);

CREATE TABLE pago (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    lote_id                BIGINT,
    linea                  INT,
    medio_cobro_id         BIGINT         NOT NULL,
    afiliado_id            BIGINT,
    nro_afiliado_informado VARCHAR(20),
    fecha_pago             DATE,
    importe                DECIMAL(14, 2),
    referencia             VARCHAR(40),
    estado                 VARCHAR(12)    NOT NULL,
    motivo_rechazo         VARCHAR(200),
    adjunto_nombre         VARCHAR(200),
    adjunto_ruta           VARCHAR(300),
    adjunto_tipo           VARCHAR(60),
    observaciones          VARCHAR(300),
    created_at             DATETIME,
    created_by             VARCHAR(80),
    updated_at             DATETIME,
    updated_by             VARCHAR(80),
    CONSTRAINT fk_pago_lote     FOREIGN KEY (lote_id)        REFERENCES lote_cobro (id),
    CONSTRAINT fk_pago_medio    FOREIGN KEY (medio_cobro_id) REFERENCES medio_cobro (id),
    CONSTRAINT fk_pago_afiliado FOREIGN KEY (afiliado_id)    REFERENCES afiliado (id)
);
CREATE INDEX ix_pago_referencia ON pago (medio_cobro_id, referencia);

ALTER TABLE imputacion    ADD CONSTRAINT fk_imput_pago FOREIGN KEY (pago_id) REFERENCES pago (id);
ALTER TABLE movimiento_cc ADD CONSTRAINT fk_mov_pago   FOREIGN KEY (pago_id) REFERENCES pago (id);
