-- =====================================================================
-- Catálogos iniciales relevados de los sistemas actuales del CMM
-- (capturas de Carga de Datos, Facturación y Cimesa).
-- Los valores marcados "A CONFIRMAR" se reemplazan cuando el CMM entregue
-- la información oficial (ver "Insumos externos" en el plan de trabajo).
-- =====================================================================

INSERT INTO numerador (clave, ultimo) VALUES ('AFILIADO_BASE', 1310900000);
INSERT INTO numerador (clave, ultimo) VALUES ('COMPROBANTE_CUOTA', 0);

-- Áreas y sectores (Facturación Individual: Área "Cimesa", Sector "Afiliados Voluntarios Particulares")
INSERT INTO area (codigo, nombre) VALUES ('CIMESA', 'Cimesa');
INSERT INTO sector (area_id, codigo, nombre) VALUES ((SELECT id FROM area WHERE codigo = 'CIMESA'), 'VOL-PART', 'Afiliados Voluntarios Particulares');
INSERT INTO sector (area_id, codigo, nombre) VALUES ((SELECT id FROM area WHERE codigo = 'CIMESA'), 'OBLIG', 'Afiliados Obligatorios');

-- Planes con su nivel de cobertura, tal como figuran en Cambios Programados y Datos del Afiliado.
-- (Los planes 7500/7600/9200 se agregan cuando IT confirme su cobertura.)
INSERT INTO plan (codigo, nombre, cobertura_porcentaje, activo, created_at, created_by) VALUES ('0101', 'PLAN "O" 100%', 100.00, TRUE, CURRENT_TIMESTAMP, 'migracion');
INSERT INTO plan (codigo, nombre, cobertura_porcentaje, activo, created_at, created_by) VALUES ('0104', 'PLAN "A1" 80/100', 80.00, TRUE, CURRENT_TIMESTAMP, 'migracion');
INSERT INTO plan (codigo, nombre, cobertura_porcentaje, activo, created_at, created_by) VALUES ('0106', 'PLAN "B1" 70/100', 70.00, TRUE, CURRENT_TIMESTAMP, 'migracion');
INSERT INTO plan (codigo, nombre, cobertura_porcentaje, activo, created_at, created_by) VALUES ('1400', 'COB. 100% C/ MASTER', 100.00, TRUE, CURRENT_TIMESTAMP, 'migracion');
INSERT INTO plan (codigo, nombre, cobertura_porcentaje, activo, created_at, created_by) VALUES ('4502', 'COB 80/100 MED 40%', 80.00, TRUE, CURRENT_TIMESTAMP, 'migracion');
-- "MED 40%": se interpreta como 40% de cobertura en medicamentos (A CONFIRMAR con el CMM).
INSERT INTO cobertura_rubro (plan_id, rubro, porcentaje) VALUES ((SELECT id FROM plan WHERE codigo = '4502'), 'MEDICAMENTOS', 40.00);

-- Obras sociales listadas en mesa de entradas (derivadoras). CUIT y convenio A CONFIRMAR.
INSERT INTO derivadora (nombre, convenio, activa, created_at, created_by) VALUES ('IOSFA', 'A CONFIRMAR', TRUE, CURRENT_TIMESTAMP, 'migracion');
INSERT INTO derivadora (nombre, convenio, activa, created_at, created_by) VALUES ('Jerárquicos Salud', 'A CONFIRMAR', TRUE, CURRENT_TIMESTAMP, 'migracion');
INSERT INTO derivadora (nombre, convenio, activa, created_at, created_by) VALUES ('Luis Pasteur', 'A CONFIRMAR', TRUE, CURRENT_TIMESTAMP, 'migracion');
INSERT INTO derivadora (nombre, convenio, activa, created_at, created_by) VALUES ('Medifé', 'A CONFIRMAR', TRUE, CURRENT_TIMESTAMP, 'migracion');
INSERT INTO derivadora (nombre, convenio, activa, created_at, created_by) VALUES ('Mutual Federada', 'A CONFIRMAR', TRUE, CURRENT_TIMESTAMP, 'migracion');
INSERT INTO derivadora (nombre, convenio, activa, created_at, created_by) VALUES ('OPDEA', 'A CONFIRMAR', TRUE, CURRENT_TIMESTAMP, 'migracion');
INSERT INTO derivadora (nombre, convenio, activa, created_at, created_by) VALUES ('OSADEF', 'A CONFIRMAR', TRUE, CURRENT_TIMESTAMP, 'migracion');

-- Cuentas contables PROVISORIAS: se reemplazan por el plan de cuentas de FEBBO.
INSERT INTO cuenta_contable (codigo, nombre) VALUES ('1.1.03.01', 'Deudores por cuotas (A CONFIRMAR)');
INSERT INTO cuenta_contable (codigo, nombre) VALUES ('4.1.01.01', 'Ingresos por cuotas (A CONFIRMAR)');
INSERT INTO cuenta_contable (codigo, nombre) VALUES ('4.1.01.02', 'Subsidio Supra Salud Alta Complejidad (A CONFIRMAR)');
INSERT INTO cuenta_contable (codigo, nombre) VALUES ('4.1.02.01', 'Recargos por mora (A CONFIRMAR)');
INSERT INTO cuenta_contable (codigo, nombre) VALUES ('1.1.01.01', 'Caja sede central (A CONFIRMAR)');
INSERT INTO cuenta_contable (codigo, nombre) VALUES ('1.1.02.05', 'Recaudación PagoMisCuentas (A CONFIRMAR)');
INSERT INTO cuenta_contable (codigo, nombre) VALUES ('1.1.02.06', 'Recaudación Pago Fácil (A CONFIRMAR)');
INSERT INTO cuenta_contable (codigo, nombre) VALUES ('1.1.02.07', 'Recaudación Rapipago (A CONFIRMAR)');
INSERT INTO cuenta_contable (codigo, nombre) VALUES ('1.1.02.08', 'Banco Supervielle (A CONFIRMAR)');

-- Códigos de servicio observados en el detalle de comprobantes.
INSERT INTO codigo_servicio (codigo, descripcion, tipo, cuenta_contable_id, importe_fijo, incluir_en_cuota, activo)
VALUES ('CUOTA', 'Cuota Mensual', 'CUOTA', (SELECT id FROM cuenta_contable WHERE codigo = '4.1.01.01'), NULL, FALSE, TRUE);
INSERT INTO codigo_servicio (codigo, descripcion, tipo, cuenta_contable_id, importe_fijo, incluir_en_cuota, activo)
VALUES ('SUB-ALTA', 'Subsidio Supra Salud Alta Complejidad', 'ADICIONAL', (SELECT id FROM cuenta_contable WHERE codigo = '4.1.01.02'), 1878.00, TRUE, TRUE);
INSERT INTO codigo_servicio (codigo, descripcion, tipo, cuenta_contable_id, importe_fijo, incluir_en_cuota, activo)
VALUES ('RECARGO', 'Recargo por atraso en pago de Boleta', 'RECARGO', (SELECT id FROM cuenta_contable WHERE codigo = '4.1.02.01'), NULL, FALSE, TRUE);

-- Parámetros de facturación (editables desde Configuración).
INSERT INTO parametro (clave, valor, descripcion) VALUES ('cuota.dia_vencimiento', '10', 'Día del mes en que vence la cuota');
INSERT INTO parametro (clave, valor, descripcion) VALUES ('mora.tasa_recargo_porcentaje', '1.40', 'Recargo mensual (%) sobre el saldo vencido impago al generar la cuota siguiente');
INSERT INTO parametro (clave, valor, descripcion) VALUES ('contable.cuenta_deudores', '1.1.03.01', 'Cuenta contable de deudores por cuotas');
INSERT INTO parametro (clave, valor, descripcion) VALUES ('morosos.cuotas_minimas', '1', 'Cantidad mínima de cuotas vencidas para figurar en el listado de morosos');

-- Medios de cobro (Procesar Cobros del sistema actual) y ventanilla.
INSERT INTO medio_cobro (codigo, nombre, cuenta_contable, admite_archivo, activo) VALUES ('PMC', 'PagoMisCuentas', '1.1.02.05', TRUE, TRUE);
INSERT INTO medio_cobro (codigo, nombre, cuenta_contable, admite_archivo, activo) VALUES ('PAGOFACIL', 'Pago Fácil', '1.1.02.06', TRUE, TRUE);
INSERT INTO medio_cobro (codigo, nombre, cuenta_contable, admite_archivo, activo) VALUES ('RAPIPAGO', 'Rapipago', '1.1.02.07', TRUE, TRUE);
INSERT INTO medio_cobro (codigo, nombre, cuenta_contable, admite_archivo, activo) VALUES ('SUPERVIELLE', 'Banco Supervielle', '1.1.02.08', TRUE, TRUE);
INSERT INTO medio_cobro (codigo, nombre, cuenta_contable, admite_archivo, activo) VALUES ('CAJA', 'Caja (ventanilla)', '1.1.01.01', FALSE, TRUE);

-- Diseño PROVISORIO del archivo de PagoMisCuentas (ancho fijo).
-- Registro de detalle: 'D' + nro afiliado(12) + fecha AAAAMMDD(8) + importe(12, 2 dec. implícitos) + referencia(15).
-- Se ajusta con el archivo real que entregue IT del CMM.
INSERT INTO layout_archivo (medio_cobro_id, nombre, formato, delimitador, prefijo_registro, lineas_encabezado,
                            afiliado_pos, afiliado_largo, fecha_pos, fecha_largo, fecha_formato,
                            importe_pos, importe_largo, importe_decimales_impl, referencia_pos, referencia_largo,
                            provisorio, observaciones, activo)
VALUES ((SELECT id FROM medio_cobro WHERE codigo = 'PMC'), 'PagoMisCuentas - rendición diaria', 'ANCHO_FIJO', NULL, 'D', 0,
        2, 12, 14, 8, 'yyyyMMdd',
        22, 12, 2, 34, 15,
        TRUE, 'Diseño provisorio hasta recibir un archivo real de ejemplo', TRUE);

-- Diseño PROVISORIO para Rapipago/Pago Fácil en formato delimitado por ';'
-- columnas: nro_afiliado;fecha(dd/MM/yyyy);importe;referencia  (posiciones = número de columna, desde 1)
INSERT INTO layout_archivo (medio_cobro_id, nombre, formato, delimitador, prefijo_registro, lineas_encabezado,
                            afiliado_pos, afiliado_largo, fecha_pos, fecha_largo, fecha_formato,
                            importe_pos, importe_largo, importe_decimales_impl, referencia_pos, referencia_largo,
                            provisorio, observaciones, activo)
VALUES ((SELECT id FROM medio_cobro WHERE codigo = 'RAPIPAGO'), 'Rapipago - CSV', 'DELIMITADO', ';', NULL, 1,
        1, NULL, 2, NULL, 'dd/MM/yyyy',
        3, NULL, 0, 4, NULL,
        TRUE, 'Diseño provisorio hasta recibir un archivo real de ejemplo', TRUE);
