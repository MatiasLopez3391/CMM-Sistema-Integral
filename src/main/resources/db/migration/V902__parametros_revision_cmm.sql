-- Parámetros de facturación según las respuestas del CMM del 22/11 (T45).
-- Va en el rango de catálogos porque los parámetros se crean en V900 (Flyway aplica por número de versión).

-- Recargo: tasa activa del Banco Nación. Solo se cambia si sigue el valor provisorio anterior.
UPDATE parametro SET valor = '4.20'
 WHERE clave = 'mora.tasa_recargo_porcentaje' AND valor = '1.40';
UPDATE parametro SET descripcion = 'Recargo (%) sobre la deuda vencida impaga, según la tasa activa del Banco Nación; se suma a la cuota siguiente'
 WHERE clave = 'mora.tasa_recargo_porcentaje';
UPDATE parametro SET descripcion = 'Día del mes en que vence la cuota (si es feriado o fin de semana, vence el día hábil siguiente)'
 WHERE clave = 'cuota.dia_vencimiento';
