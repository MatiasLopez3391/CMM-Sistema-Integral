-- =====================================================================
-- F4 · Control del registro de pie de los archivos de entes de cobro.
-- Los archivos reales (ej. PagoMisCuentas COB4104) cierran con un registro que informa
-- la cantidad de pagos y el importe total: se compara contra lo leído para detectar
-- archivos incompletos o dañados antes de imputar nada.
-- =====================================================================

ALTER TABLE layout_archivo ADD COLUMN pie_prefijo        VARCHAR(10);
ALTER TABLE layout_archivo ADD COLUMN pie_cantidad_pos   INT;
ALTER TABLE layout_archivo ADD COLUMN pie_cantidad_largo INT;
ALTER TABLE layout_archivo ADD COLUMN pie_total_pos      INT;
ALTER TABLE layout_archivo ADD COLUMN pie_total_largo    INT;

ALTER TABLE lote_cobro ADD COLUMN pie_controlado BOOLEAN NOT NULL DEFAULT FALSE;
