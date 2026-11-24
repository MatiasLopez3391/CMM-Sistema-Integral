
-- F5 - T27: Estructura de órdenes médicas
-- T28 utilizará las tablas de auditoría e historial.

CREATE TABLE orden (
    id BIGINT NOT NULL AUTO_INCREMENT,
    numero VARCHAR(40) NOT NULL,
    afiliado_id BIGINT NOT NULL,
    prestador_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    fecha_emision DATE NOT NULL,
    estado VARCHAR(30) NOT NULL,
    total_valor_convenido DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_coseguro DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_cargo_entidad DECIMAL(15,2) NOT NULL DEFAULT 0,
    emitida_por VARCHAR(100) NOT NULL,
    created_at DATETIME,
    created_by VARCHAR(255),
    updated_at DATETIME,
    updated_by VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_orden_numero UNIQUE (numero),
    CONSTRAINT fk_orden_afiliado
        FOREIGN KEY (afiliado_id) REFERENCES afiliado(id),
    CONSTRAINT fk_orden_prestador
        FOREIGN KEY (prestador_id) REFERENCES prestador(id),
    CONSTRAINT fk_orden_plan
        FOREIGN KEY (plan_id) REFERENCES plan(id)
);

CREATE TABLE orden_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    orden_id BIGINT NOT NULL,
    practica_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    valor_convenido DECIMAL(15,2) NOT NULL,
    porcentaje_cobertura DECIMAL(10,4) NOT NULL,
    cargo_entidad DECIMAL(15,2) NOT NULL,
    coseguro_afiliado DECIMAL(15,2) NOT NULL,
    requiere_auditoria BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_orden_item_orden
        FOREIGN KEY (orden_id) REFERENCES orden(id),
    CONSTRAINT fk_orden_item_practica
        FOREIGN KEY (practica_id) REFERENCES practica(id),
    CONSTRAINT chk_orden_item_cantidad CHECK (cantidad > 0)
);

CREATE TABLE orden_estado_historial (
    id BIGINT NOT NULL AUTO_INCREMENT,
    orden_id BIGINT NOT NULL,
    estado_anterior VARCHAR(30),
    estado_nuevo VARCHAR(30) NOT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario VARCHAR(100) NOT NULL,
    comentario VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT fk_orden_historial_orden
        FOREIGN KEY (orden_id) REFERENCES orden(id)
);

CREATE TABLE auditoria_medica (
    id BIGINT NOT NULL AUTO_INCREMENT,
    orden_id BIGINT NOT NULL,
    usuario_auditor VARCHAR(100) NOT NULL,
    decision VARCHAR(30) NOT NULL,
    comentario VARCHAR(2000),
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_auditoria_medica_orden
        FOREIGN KEY (orden_id) REFERENCES orden(id)
);

CREATE INDEX idx_orden_afiliado
    ON orden(afiliado_id);

CREATE INDEX idx_orden_estado
    ON orden(estado);

CREATE INDEX idx_orden_fecha
    ON orden(fecha_emision);

CREATE INDEX idx_orden_historial
    ON orden_estado_historial(orden_id, fecha);

CREATE INDEX idx_auditoria_medica_orden
    ON auditoria_medica(orden_id);

INSERT INTO numerador (clave, ultimo)
VALUES ('ORDEN', 0);
