-- =====================================================================
-- F3 · Notas de crédito y débito internas (T46)
-- Respuestas L6 y L7 del CMM: los errores se corrigen con notas de crédito o débito.
-- =====================================================================

-- Un grupo puede tener varias notas en el mismo período: la unicidad (grupo, período, tipo) deja de
-- valer para todos los tipos y la cuota la controla la generación. Primero se crea un índice que
-- sirva a la clave foránea del afiliado y después se quita la restricción única.
CREATE INDEX ix_comprobante_grupo_periodo ON comprobante (afiliado_id, periodo, tipo);
ALTER TABLE comprobante DROP CONSTRAINT uq_comprobante_periodo;

-- Comprobante que corrige la nota (opcional).
ALTER TABLE comprobante ADD COLUMN comprobante_asociado_id BIGINT;
ALTER TABLE comprobante ADD CONSTRAINT fk_comprobante_asociado FOREIGN KEY (comprobante_asociado_id) REFERENCES comprobante (id);

-- Qué parte de una nota de crédito canceló qué comprobante (equivalente a imputacion para los pagos).
CREATE TABLE aplicacion_credito (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nota_credito_id BIGINT         NOT NULL,
    comprobante_id  BIGINT         NOT NULL,
    importe         DECIMAL(14, 2) NOT NULL,
    fecha           DATE           NOT NULL,
    CONSTRAINT fk_aplic_nota        FOREIGN KEY (nota_credito_id) REFERENCES comprobante (id),
    CONSTRAINT fk_aplic_comprobante FOREIGN KEY (comprobante_id)  REFERENCES comprobante (id)
);
CREATE INDEX ix_aplic_nota ON aplicacion_credito (nota_credito_id);
CREATE INDEX ix_aplic_comprobante ON aplicacion_credito (comprobante_id);

INSERT INTO numerador (clave, ultimo) VALUES ('NOTA_CREDITO', 0);
INSERT INTO numerador (clave, ultimo) VALUES ('NOTA_DEBITO', 0);
