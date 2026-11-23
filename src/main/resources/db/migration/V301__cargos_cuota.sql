-- =====================================================================
-- F3 · Coseguros que se suman a la cuota del mes siguiente (T42).
-- Cada orden emitida (F5) o cada carga manual de Liquidaciones deja un cargo
-- PENDIENTE a nombre del integrante; la generación de cuotas lo incluye en el
-- comprobante del grupo y lo pasa a FACTURADO.
-- =====================================================================

CREATE TABLE cargo_cuota (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    titular_id       BIGINT         NOT NULL,
    afiliado_id      BIGINT         NOT NULL,
    tipo             VARCHAR(20)    NOT NULL,
    concepto         VARCHAR(200)   NOT NULL,
    fecha            DATE           NOT NULL,
    importe          DECIMAL(14, 2) NOT NULL,
    periodo_destino  VARCHAR(6)     NOT NULL,
    origen           VARCHAR(20)    NOT NULL,
    origen_ref       VARCHAR(60),
    estado           VARCHAR(12)    NOT NULL,
    comprobante_id   BIGINT,
    motivo_anulacion VARCHAR(200),
    created_at       DATETIME,
    created_by       VARCHAR(80),
    updated_at       DATETIME,
    updated_by       VARCHAR(80),
    CONSTRAINT fk_cargo_titular     FOREIGN KEY (titular_id)     REFERENCES afiliado (id),
    CONSTRAINT fk_cargo_afiliado    FOREIGN KEY (afiliado_id)    REFERENCES afiliado (id),
    CONSTRAINT fk_cargo_comprobante FOREIGN KEY (comprobante_id) REFERENCES comprobante (id),
    -- Una orden genera un solo cargo (evita duplicados si F5 reintenta)
    CONSTRAINT uq_cargo_origen UNIQUE (origen, origen_ref)
);
CREATE INDEX ix_cargo_pendiente ON cargo_cuota (estado, periodo_destino);
CREATE INDEX ix_cargo_titular ON cargo_cuota (titular_id, estado);

-- Cuenta y código de servicio del coseguro (cuenta provisoria hasta recibir el plan de cuentas del CMM).
INSERT INTO cuenta_contable (codigo, nombre) VALUES ('4.1.03.01', 'Ingresos por coseguros');
INSERT INTO codigo_servicio (codigo, descripcion, tipo, cuenta_contable_id, importe_fijo, incluir_en_cuota, activo)
VALUES ('COSEGURO', 'Coseguro', 'COSEGURO', (SELECT id FROM cuenta_contable WHERE codigo = '4.1.03.01'), NULL, FALSE, TRUE);
