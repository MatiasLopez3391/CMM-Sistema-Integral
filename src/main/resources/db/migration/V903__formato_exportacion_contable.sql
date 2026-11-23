-- Formato del archivo de asientos para el sistema contable (T48). Configurable desde la pantalla
-- "Exportación a sistema contable" para adaptarlo al sistema que use el CMM, sin cambiar el código.
INSERT INTO parametro (clave, valor, descripcion) VALUES
 ('contable.export.columnas', 'nro_asiento,fecha,cuenta,debe,haber,concepto,referencia',
  'Columnas del archivo de asientos, en orden (nro_asiento, fecha, cuenta, debe, haber, importe, dh, concepto, referencia)'),
 ('contable.export.separador', ';', 'Separador de columnas del archivo de asientos (un carácter, o TAB)'),
 ('contable.export.formato_fecha', 'dd/MM/yyyy', 'Formato de fecha del archivo de asientos (ej. dd/MM/yyyy, yyyyMMdd)'),
 ('contable.export.separador_decimal', '.', 'Separador decimal de los importes del archivo de asientos (. o ,)'),
 ('contable.export.encabezado', 'SI', 'Si la primera línea del archivo de asientos lleva los nombres de las columnas (SI o NO)'),
 ('contable.export.extension', 'csv', 'Extensión del archivo de asientos (csv o txt)');
