-- F5 · T28, T30, T66: motivo de auditoría, vencimiento, autorización parcial y anulación.

-- Por qué la orden fue a auditoría (práctica que lo requiere, tope mensual o pedido del médico).
ALTER TABLE orden ADD COLUMN motivo_auditoria VARCHAR(300);
-- Médico que prescribe e indicación (diagnóstico u observaciones del pedido).
ALTER TABLE orden ADD COLUMN medico_solicitante VARCHAR(150);
ALTER TABLE orden ADD COLUMN observaciones VARCHAR(1000);
-- Vence a los N meses de emitida o autorizada (parámetro ordenes.meses_vencimiento). Vacío mientras está en auditoría.
ALTER TABLE orden ADD COLUMN fecha_vencimiento DATE;
-- Fecha y hora de la última decisión del auditor: el aviso a Mesa de Entradas se arma con este dato.
ALTER TABLE orden ADD COLUMN fecha_resolucion DATETIME;

-- Autorización parcial: el auditor puede bajar la cantidad de una práctica o no autorizarla (cantidad 0).
-- cantidad_solicitada guarda lo que se pidió; cantidad, lo autorizado.
ALTER TABLE orden_item ADD COLUMN cantidad_solicitada INT;
UPDATE orden_item SET cantidad_solicitada = cantidad;
ALTER TABLE orden_item DROP CONSTRAINT chk_orden_item_cantidad;

CREATE INDEX idx_orden_resolucion ON orden(fecha_resolucion);
