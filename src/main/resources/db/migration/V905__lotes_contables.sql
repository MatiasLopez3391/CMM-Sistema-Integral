-- Lotes del sistema contable (respuesta del CMM del 23/11): cada tipo de comprobante fiscal va en su lote.
-- Factura A = 1, Nota de débito A = 2, Nota de crédito A = 3, Factura B = 6, Nota de crédito B = 8.
-- Nota de débito B y cobranzas: a confirmar (quedan vacíos hasta que el CMM los informe).
INSERT INTO parametro (clave, valor, descripcion) VALUES
 ('contable.letra_por_defecto', 'B', 'Letra de comprobante para los afiliados (B: consumidor final). La A requiere la condición frente al IVA del titular'),
 ('contable.lote.CUOTA.A', '1', 'Lote del sistema contable para facturas A (cuotas)'),
 ('contable.lote.NOTA_DEBITO.A', '2', 'Lote del sistema contable para notas de débito A'),
 ('contable.lote.NOTA_CREDITO.A', '3', 'Lote del sistema contable para notas de crédito A'),
 ('contable.lote.CUOTA.B', '6', 'Lote del sistema contable para facturas B (cuotas)'),
 ('contable.lote.NOTA_DEBITO.B', '-', 'Lote del sistema contable para notas de débito B (a confirmar; - = sin lote)'),
 ('contable.lote.NOTA_CREDITO.B', '8', 'Lote del sistema contable para notas de crédito B'),
 ('contable.lote.PAGO', '-', 'Lote del sistema contable para las cobranzas (a confirmar; - = sin lote)');

-- La columna "lote" pasa a ser la primera del archivo, salvo que el formato ya se haya cambiado.
UPDATE parametro SET valor = 'lote,nro_asiento,fecha,cuenta,debe,haber,concepto,referencia'
 WHERE clave = 'contable.export.columnas' AND valor = 'nro_asiento,fecha,cuenta,debe,haber,concepto,referencia';
UPDATE parametro SET descripcion = 'Columnas del archivo de asientos, en orden (lote, nro_asiento, fecha, cuenta, debe, haber, importe, dh, concepto, referencia)'
 WHERE clave = 'contable.export.columnas';
