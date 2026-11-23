-- =====================================================================
-- F4 · Diseño REAL del archivo de PagoMisCuentas (COB4104.AAMMDD), reemplaza al provisorio de V900.
-- Se entrega como migración de catálogo (V9xx) para que corra DESPUÉS de V900 en bases nuevas.
--
-- Registros de 100 caracteres, posiciones desde 1:
--   Encabezado  '0'  (1)  + código de ente/empresa + AAAAMMDD de generación
--   Detalle     '5'  (1)
--               2-20   nro. de afiliado, 12 dígitos y relleno con espacios
--               21-40  identificador de factura/pago (se usa como referencia, evita duplicados)
--               41-48  fecha de cobro AAAAMMDD
--               58-68  importe, 11 dígitos con 2 decimales implícitos
--   Pie         '9'  (1)
--               17-23  cantidad de registros de detalle
--               24-41  importe total, 2 decimales implícitos
-- Verificado con un archivo de 4 pagos cuyo pie coincide (cantidad y total).
-- Los demás campos del detalle (49-57 y 69-100) no se usan: parecen fecha de acreditación y código de lote.
-- =====================================================================

UPDATE layout_archivo
   SET nombre                 = 'PagoMisCuentas - archivo COB',
       formato                = 'ANCHO_FIJO',
       delimitador            = NULL,
       prefijo_registro       = '5',
       lineas_encabezado      = 0,
       afiliado_pos           = 2,
       afiliado_largo         = 19,
       fecha_pos              = 41,
       fecha_largo            = 8,
       fecha_formato          = 'yyyyMMdd',
       importe_pos            = 58,
       importe_largo          = 11,
       importe_decimales_impl = 2,
       referencia_pos         = 21,
       referencia_largo       = 20,
       pie_prefijo            = '9',
       pie_cantidad_pos       = 17,
       pie_cantidad_largo     = 7,
       pie_total_pos          = 24,
       pie_total_largo        = 18,
       provisorio             = FALSE,
       observaciones          = 'Diseño real según archivo COB4104.111126 (11/11/2026). Pie controlado: cantidad y total.'
 WHERE medio_cobro_id = (SELECT id FROM medio_cobro WHERE codigo = 'PMC');
