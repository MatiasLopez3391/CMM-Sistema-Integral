-- Proceso de mora, leyenda de deuda y texto del aviso (T50, T51, T52), según las respuestas L10, L11 y L12 del CMM.

-- El texto del aviso no entra en 200 caracteres.
ALTER TABLE parametro MODIFY valor VARCHAR(2000) NOT NULL;

INSERT INTO parametro (clave, valor, descripcion) VALUES
 ('mora.dia_bloqueo', '20', 'Día del mes desde el que se propone bloquear a quien no pagó la cuota (el bloqueo lo confirma un usuario)'),
 ('mora.cuotas_baja', '3', 'Cuotas vencidas impagas desde las que el grupo figura para dar de baja (la baja es siempre manual)'),
 ('cuota.leyenda_deuda', 'Usted registra deuda vencida por {deuda} al {fecha}. Regularice su situación para evitar ser informado en el Clearing.',
  'Leyenda que se imprime en la cuota de quien tiene deuda vencida. Admite {deuda} y {fecha}'),
 ('aviso.deuda.asunto', 'Círculo Médico de Mendoza - Aviso de deuda', 'Asunto del mail de aviso de deuda'),
 ('aviso.deuda.texto', 'Estimado/a {nombre}:\n\nLe informamos que al {fecha} registra {cuotas} cuota(s) vencida(s) por un total de {deuda}.\nLa deuda más antigua venció el {vencimiento}.\n\nPuede abonar por PagoMisCuentas, Pago Fácil, Rapipago o en nuestras oficinas.\nSi ya realizó el pago, desestime este mensaje.\n\nDepartamento de Cobranzas - Círculo Médico de Mendoza',
  'Texto del mail de aviso de deuda. Admite {nombre}, {fecha}, {cuotas}, {deuda} y {vencimiento}');
