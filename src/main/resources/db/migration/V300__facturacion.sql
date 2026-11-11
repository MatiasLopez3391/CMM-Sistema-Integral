-- =====================================================================
-- F3 · Cuotas y Cuenta Corriente (rango V300-V399)
-- Sin factura fiscal (H2): la cuota se emite como comprobante interno.
-- =====================================================================

CREATE TABLE cuenta_contable (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(20)  NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL
);

-- Código de servicio -> cuenta contable (base de la exportación a FEBBO).
CREATE TABLE codigo_servicio (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo             VARCHAR(20)    NOT NULL UNIQUE,
    descripcion        VARCHAR(120)   NOT NULL,
    tipo               VARCHAR(20)    NOT NULL,
    cuenta_contable_id BIGINT         NOT NULL,
    importe_fijo       DECIMAL(14, 2),
    incluir_en_cuota   BOOLEAN        NOT NULL DEFAULT FALSE,
    activo             BOOLEAN        NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_codserv_cuenta FOREIGN KEY (cuenta_contable_id) REFERENCES cuenta_contable (id)
);

-- Parámetros editables desde pantalla (tasa de recargo, día de vencimiento...).
CREATE TABLE parametro (
    clave       VARCHAR(60)  NOT NULL PRIMARY KEY,
    valor       VARCHAR(200) NOT NULL,
    descripcion VARCHAR(250)
);

-- Valor de la cuota por integrante según plan, con vigencia.
-- La actualización mensual crea una nueva vigencia (sin pedirlo a IT).
CREATE TABLE valor_cuota (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id       BIGINT         NOT NULL,
    vigente_desde DATE           NOT NULL,
    importe       DECIMAL(14, 2) NOT NULL,
    motivo        VARCHAR(150),
    created_at    DATETIME,
    created_by    VARCHAR(80),
    updated_at    DATETIME,
    updated_by    VARCHAR(80),
    CONSTRAINT fk_valcuota_plan FOREIGN KEY (plan_id) REFERENCES plan (id),
    CONSTRAINT uq_valcuota UNIQUE (plan_id, vigente_desde)
);

CREATE TABLE comprobante (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo              VARCHAR(20)    NOT NULL,
    numero            VARCHAR(20)    NOT NULL UNIQUE,
    afiliado_id       BIGINT         NOT NULL,
    periodo           VARCHAR(6)     NOT NULL,
    fecha_emision     DATE           NOT NULL,
    fecha_vencimiento DATE           NOT NULL,
    total             DECIMAL(14, 2) NOT NULL,
    saldo             DECIMAL(14, 2) NOT NULL,
    estado            VARCHAR(12)    NOT NULL,
    observaciones     VARCHAR(300),
    created_at        DATETIME,
    created_by        VARCHAR(80),
    updated_at        DATETIME,
    updated_by        VARCHAR(80),
    CONSTRAINT fk_comprobante_afiliado FOREIGN KEY (afiliado_id) REFERENCES afiliado (id),
    CONSTRAINT uq_comprobante_periodo UNIQUE (afiliado_id, periodo, tipo)
);
CREATE INDEX ix_comprobante_vto ON comprobante (estado, fecha_vencimiento);

CREATE TABLE comprobante_item (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    comprobante_id     BIGINT         NOT NULL,
    codigo_servicio_id BIGINT         NOT NULL,
    descripcion        VARCHAR(200)   NOT NULL,
    cantidad           INT            NOT NULL,
    precio_unitario    DECIMAL(14, 2) NOT NULL,
    importe            DECIMAL(14, 2) NOT NULL,
    CONSTRAINT fk_item_comprobante FOREIGN KEY (comprobante_id)     REFERENCES comprobante (id),
    CONSTRAINT fk_item_codserv     FOREIGN KEY (codigo_servicio_id) REFERENCES codigo_servicio (id)
);

CREATE TABLE movimiento_cc (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    afiliado_id    BIGINT         NOT NULL,
    fecha          DATE           NOT NULL,
    tipo           VARCHAR(5)     NOT NULL,
    concepto       VARCHAR(200)   NOT NULL,
    comprobante_id BIGINT,
    pago_id        BIGINT,
    -- Para pagos: cuenta contable del medio de cobro (contrapartida del asiento FEBBO)
    cuenta_contrapartida VARCHAR(20),
    importe        DECIMAL(14, 2) NOT NULL,
    created_at     DATETIME,
    created_by     VARCHAR(80),
    updated_at     DATETIME,
    updated_by     VARCHAR(80),
    CONSTRAINT fk_mov_afiliado    FOREIGN KEY (afiliado_id)    REFERENCES afiliado (id),
    CONSTRAINT fk_mov_comprobante FOREIGN KEY (comprobante_id) REFERENCES comprobante (id)
);
CREATE INDEX ix_mov_afiliado ON movimiento_cc (afiliado_id, fecha);

CREATE TABLE imputacion (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    pago_id        BIGINT         NOT NULL,
    comprobante_id BIGINT         NOT NULL,
    importe        DECIMAL(14, 2) NOT NULL,
    fecha          DATE           NOT NULL,
    CONSTRAINT fk_imput_comprobante FOREIGN KEY (comprobante_id) REFERENCES comprobante (id)
);

CREATE TABLE notificacion (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    afiliado_id    BIGINT,
    destinatario   VARCHAR(120),
    asunto         VARCHAR(200)  NOT NULL,
    cuerpo         VARCHAR(4000) NOT NULL,
    tipo           VARCHAR(30)   NOT NULL,
    estado         VARCHAR(20)   NOT NULL,
    error          VARCHAR(500),
    fecha_creacion DATETIME      NOT NULL,
    fecha_envio    DATETIME,
    CONSTRAINT fk_notif_afiliado FOREIGN KEY (afiliado_id) REFERENCES afiliado (id)
);
