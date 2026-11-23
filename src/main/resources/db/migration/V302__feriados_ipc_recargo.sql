-- =====================================================================
-- F3 · Vencimiento hábil e IPC (T44, T45)
-- Respuestas del CMM (22/11): la cuota vence el día 10 o el hábil siguiente;
-- el recargo (tasa activa del Banco Nación) se ajusta en V902, después de los catálogos;
-- la cuota se actualiza por IPC mensual (API y carga manual).
-- =====================================================================

-- Días no hábiles para correr vencimientos (feriados nacionales y no laborables con fines turísticos).
CREATE TABLE feriado (
    fecha       DATE         NOT NULL PRIMARY KEY,
    descripcion VARCHAR(150) NOT NULL,
    origen      VARCHAR(20)  NOT NULL,
    created_at  DATETIME,
    created_by  VARCHAR(80),
    updated_at  DATETIME,
    updated_by  VARCHAR(80)
);

-- Feriados nacionales 2026 (fuente: calendario oficial publicado por el Gobierno nacional).
INSERT INTO feriado (fecha, descripcion, origen, created_at, created_by) VALUES
 (DATE '2026-01-01', 'Año Nuevo', 'INICIAL', CURRENT_TIMESTAMP, 'migracion'),
 (DATE '2026-02-16', 'Carnaval', 'INICIAL', CURRENT_TIMESTAMP, 'migracion'),
 (DATE '2026-02-17', 'Carnaval', 'INICIAL', CURRENT_TIMESTAMP, 'migracion'),
 (DATE '2026-03-23', 'Día no laborable con fines turísticos', 'INICIAL', CURRENT_TIMESTAMP, 'migracion'),
 (DATE '2026-03-24', 'Día Nacional de la Memoria por la Verdad y la Justicia', 'INICIAL', CURRENT_TIMESTAMP, 'migracion'),
 (DATE '2026-04-02', 'Día del Veterano y de los Caídos en la Guerra de Malvinas', 'INICIAL', CURRENT_TIMESTAMP, 'migracion'),
 (DATE '2026-04-03', 'Viernes Santo', 'INICIAL', CURRENT_TIMESTAMP, 'migracion'),
 (DATE '2026-05-01', 'Día del Trabajador', 'INICIAL', CURRENT_TIMESTAMP, 'migracion'),
 (DATE '2026-05-25', 'Día de la Revolución de Mayo', 'INICIAL', CURRENT_TIMESTAMP, 'migracion'),
 (DATE '2026-06-15', 'Paso a la Inmortalidad del Gral. Martín Miguel de Güemes', 'INICIAL', CURRENT_TIMESTAMP, 'migracion'),
 (DATE '2026-06-20', 'Paso a la Inmortalidad del Gral. Manuel Belgrano', 'INICIAL', CURRENT_TIMESTAMP, 'migracion'),
 (DATE '2026-07-09', 'Día de la Independencia', 'INICIAL', CURRENT_TIMESTAMP, 'migracion'),
 (DATE '2026-07-10', 'Día no laborable con fines turísticos', 'INICIAL', CURRENT_TIMESTAMP, 'migracion'),
 (DATE '2026-08-17', 'Paso a la Inmortalidad del Gral. José de San Martín', 'INICIAL', CURRENT_TIMESTAMP, 'migracion'),
 (DATE '2026-10-12', 'Día del Respeto a la Diversidad Cultural', 'INICIAL', CURRENT_TIMESTAMP, 'migracion'),
 (DATE '2026-11-23', 'Día de la Soberanía Nacional', 'INICIAL', CURRENT_TIMESTAMP, 'migracion'),
 (DATE '2026-12-07', 'Día no laborable con fines turísticos', 'INICIAL', CURRENT_TIMESTAMP, 'migracion'),
 (DATE '2026-12-08', 'Día de la Inmaculada Concepción de María', 'INICIAL', CURRENT_TIMESTAMP, 'migracion'),
 (DATE '2026-12-25', 'Navidad', 'INICIAL', CURRENT_TIMESTAMP, 'migracion');

-- Variación mensual del IPC (INDEC), traída de la API de series de tiempo o cargada a mano.
CREATE TABLE indice_ipc (
    periodo           VARCHAR(6)    NOT NULL PRIMARY KEY,
    variacion         DECIMAL(7, 3) NOT NULL,
    origen            VARCHAR(20)   NOT NULL,
    aplicado_vigencia DATE,
    created_at        DATETIME,
    created_by        VARCHAR(80),
    updated_at        DATETIME,
    updated_by        VARCHAR(80)
);
