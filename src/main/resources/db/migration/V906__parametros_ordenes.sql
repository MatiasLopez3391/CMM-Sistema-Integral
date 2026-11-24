-- F5 · Reglas de la orden (respuestas M7 y M11 del CMM), editables desde Configuración.
INSERT INTO parametro (clave, valor, descripcion) VALUES
 ('ordenes.tope_mensual', '3', 'Órdenes por afiliado en el mes: la que supera este número va a auditoría médica'),
 ('ordenes.meses_vencimiento', '1', 'Meses de validez de una orden desde que se emite o se autoriza');
