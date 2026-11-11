-- =====================================================================
-- DATOS DE DEMOSTRACIÓN (solo perfil "demo"). Personas ficticias.
-- Valores de prestadores tomados como referencia de la planilla
-- "Agosto 2026 Clínica Santa Rosa" observada en el relevamiento.
-- =====================================================================

-- ---------- Afiliados (titulares) ----------
INSERT INTO afiliado (nro_afiliado, titular_id, parentesco, apellido, nombre, tipo_documento, documento, cuil, fecha_nacimiento, sexo, email, telefono, domicilio, localidad, plan_id, area_id, sector_id, derivadora_id, via_cobro, carencia, estado, fecha_alta, bloqueado, created_at, created_by)
VALUES ('131090000100', NULL, 'TITULAR', 'GÓMEZ', 'Laura Beatriz', 'DNI', '28455120', '27-28455120-4', DATE '1980-03-14', 'F', 'laura.gomez@example.com', '261-4410020', 'Av. San Martín 1520', 'Mendoza',
        (SELECT id FROM plan WHERE codigo='0104'), (SELECT id FROM area WHERE codigo='CIMESA'), (SELECT id FROM sector WHERE codigo='VOL-PART'), NULL, 'PMC', 'COMUN', 'ACTIVO', DATE '2015-06-01', FALSE, CURRENT_TIMESTAMP, 'demo');
INSERT INTO afiliado (nro_afiliado, titular_id, parentesco, apellido, nombre, tipo_documento, documento, cuil, fecha_nacimiento, sexo, email, telefono, domicilio, localidad, plan_id, area_id, sector_id, derivadora_id, via_cobro, carencia, estado, fecha_alta, bloqueado, created_at, created_by)
VALUES ('131090000200', NULL, 'TITULAR', 'FERNÁNDEZ', 'Ricardo Daniel', 'DNI', '22310987', '20-22310987-1', DATE '1971-11-02', 'M', 'rfernandez@example.com', '261-5523301', 'Belgrano 845', 'Godoy Cruz',
        (SELECT id FROM plan WHERE codigo='0106'), (SELECT id FROM area WHERE codigo='CIMESA'), (SELECT id FROM sector WHERE codigo='VOL-PART'), NULL, 'PMC', 'COMUN', 'ACTIVO', DATE '2009-02-01', FALSE, CURRENT_TIMESTAMP, 'demo');
INSERT INTO afiliado (nro_afiliado, titular_id, parentesco, apellido, nombre, tipo_documento, documento, cuil, fecha_nacimiento, sexo, email, telefono, domicilio, localidad, plan_id, area_id, sector_id, derivadora_id, via_cobro, carencia, estado, fecha_alta, bloqueado, created_at, created_by)
VALUES ('131090000300', NULL, 'TITULAR', 'ROSSI', 'Carla Andrea', 'DNI', '30112456', '27-30112456-8', DATE '1983-07-21', 'F', 'carla.rossi@example.com', '261-4987766', 'Patricias Mendocinas 1120', 'Mendoza',
        (SELECT id FROM plan WHERE codigo='0101'), (SELECT id FROM area WHERE codigo='CIMESA'), (SELECT id FROM sector WHERE codigo='VOL-PART'), NULL, 'CAJA', 'COMUN', 'ACTIVO', DATE '2018-09-01', FALSE, CURRENT_TIMESTAMP, 'demo');
INSERT INTO afiliado (nro_afiliado, titular_id, parentesco, apellido, nombre, tipo_documento, documento, cuil, fecha_nacimiento, sexo, email, telefono, domicilio, localidad, plan_id, area_id, sector_id, derivadora_id, via_cobro, carencia, estado, fecha_alta, bloqueado, created_at, created_by)
VALUES ('131090000400', NULL, 'TITULAR', 'ÁLVAREZ', 'Diego Martín', 'DNI', '33874120', '20-33874120-6', DATE '1988-01-30', 'M', 'dalvarez@example.com', '261-6154432', 'Rivadavia 230', 'Luján de Cuyo',
        (SELECT id FROM plan WHERE codigo='4502'), (SELECT id FROM area WHERE codigo='CIMESA'), (SELECT id FROM sector WHERE codigo='OBLIG'), (SELECT id FROM derivadora WHERE nombre='Medifé'), 'PMC', 'REDUCIDA', 'ACTIVO', DATE '2022-03-01', FALSE, CURRENT_TIMESTAMP, 'demo');
INSERT INTO afiliado (nro_afiliado, titular_id, parentesco, apellido, nombre, tipo_documento, documento, cuil, fecha_nacimiento, sexo, email, telefono, domicilio, localidad, plan_id, area_id, sector_id, derivadora_id, via_cobro, carencia, estado, fecha_alta, bloqueado, created_at, created_by)
VALUES ('131090000500', NULL, 'TITULAR', 'MOLINA', 'Patricia Inés', 'DNI', '25667091', '27-25667091-3', DATE '1976-05-09', 'F', 'pmolina@example.com', '261-4225511', 'Colón 455', 'Mendoza',
        (SELECT id FROM plan WHERE codigo='0104'), (SELECT id FROM area WHERE codigo='CIMESA'), (SELECT id FROM sector WHERE codigo='VOL-PART'), NULL, 'PMC', 'COMUN', 'ACTIVO', DATE '2012-04-01', FALSE, CURRENT_TIMESTAMP, 'demo');
INSERT INTO afiliado (nro_afiliado, titular_id, parentesco, apellido, nombre, tipo_documento, documento, cuil, fecha_nacimiento, sexo, email, telefono, domicilio, localidad, plan_id, area_id, sector_id, derivadora_id, via_cobro, carencia, estado, fecha_alta, bloqueado, created_at, created_by)
VALUES ('131090000600', NULL, 'TITULAR', 'SUÁREZ', 'Hernán José', 'DNI', '27109344', '20-27109344-9', DATE '1979-09-17', 'M', 'hsuarez@example.com', '261-5098712', 'Perú 1789', 'Guaymallén',
        (SELECT id FROM plan WHERE codigo='0106'), (SELECT id FROM area WHERE codigo='CIMESA'), (SELECT id FROM sector WHERE codigo='VOL-PART'), NULL, 'RAPIPAGO', 'COMUN', 'ACTIVO', DATE '2016-11-01', FALSE, CURRENT_TIMESTAMP, 'demo');
INSERT INTO afiliado (nro_afiliado, titular_id, parentesco, apellido, nombre, tipo_documento, documento, cuil, fecha_nacimiento, sexo, email, telefono, domicilio, localidad, plan_id, area_id, sector_id, derivadora_id, via_cobro, carencia, estado, fecha_alta, bloqueado, created_at, created_by)
VALUES ('131090000700', NULL, 'TITULAR', 'CASTRO', 'Julieta María', 'DNI', '35221678', '27-35221678-0', DATE '1990-12-03', 'F', 'jcastro@example.com', '261-4776654', 'Sarmiento 66', 'Mendoza',
        (SELECT id FROM plan WHERE codigo='1400'), (SELECT id FROM area WHERE codigo='CIMESA'), (SELECT id FROM sector WHERE codigo='VOL-PART'), NULL, 'PMC', 'COMUN', 'ACTIVO', DATE '2020-01-01', FALSE, CURRENT_TIMESTAMP, 'demo');
INSERT INTO afiliado (nro_afiliado, titular_id, parentesco, apellido, nombre, tipo_documento, documento, cuil, fecha_nacimiento, sexo, email, telefono, domicilio, localidad, plan_id, area_id, sector_id, derivadora_id, via_cobro, carencia, estado_especial, estado_especial_desde, estado_especial_hasta, estado, fecha_alta, bloqueado, created_at, created_by)
VALUES ('131090000800', NULL, 'TITULAR', 'ORTIZ', 'Gustavo Raúl', 'DNI', '20456789', '20-20456789-5', DATE '1968-04-25', 'M', 'gortiz@example.com', '261-4334411', 'Las Heras 990', 'Las Heras',
        (SELECT id FROM plan WHERE codigo='0106'), (SELECT id FROM area WHERE codigo='CIMESA'), (SELECT id FROM sector WHERE codigo='OBLIG'), (SELECT id FROM derivadora WHERE nombre='OSADEF'), 'PMC', 'COMUN', 'Discapacidad - auditar', DATE '2026-01-01', DATE '2026-12-31', 'ACTIVO', DATE '2010-08-01', FALSE, CURRENT_TIMESTAMP, 'demo');
INSERT INTO afiliado (nro_afiliado, titular_id, parentesco, apellido, nombre, tipo_documento, documento, cuil, fecha_nacimiento, sexo, email, telefono, domicilio, localidad, plan_id, area_id, sector_id, derivadora_id, via_cobro, carencia, estado, fecha_alta, bloqueado, created_at, created_by)
VALUES ('131090000900', NULL, 'TITULAR', 'NAVARRO', 'Silvia Elena', 'DNI', '29887412', '27-29887412-2', DATE '1982-02-11', 'F', 'snavarro@example.com', '261-6012233', 'Mitre 1345', 'Maipú',
        (SELECT id FROM plan WHERE codigo='0104'), (SELECT id FROM area WHERE codigo='CIMESA'), (SELECT id FROM sector WHERE codigo='VOL-PART'), NULL, 'PMC', 'COMUN', 'ACTIVO', DATE '2014-05-01', FALSE, CURRENT_TIMESTAMP, 'demo');
INSERT INTO afiliado (nro_afiliado, titular_id, parentesco, apellido, nombre, tipo_documento, documento, cuil, fecha_nacimiento, sexo, email, telefono, domicilio, localidad, plan_id, area_id, sector_id, derivadora_id, via_cobro, carencia, estado, fecha_alta, bloqueado, created_at, created_by)
VALUES ('131090001000', NULL, 'TITULAR', 'RÍOS', 'Esteban Luis', 'DNI', '24001562', '20-24001562-7', DATE '1974-10-08', 'M', 'erios@example.com', '261-4558899', 'Chile 1210', 'Mendoza',
        (SELECT id FROM plan WHERE codigo='0101'), (SELECT id FROM area WHERE codigo='CIMESA'), (SELECT id FROM sector WHERE codigo='VOL-PART'), NULL, 'CAJA', 'COMUN', 'ACTIVO', DATE '2011-03-01', FALSE, CURRENT_TIMESTAMP, 'demo');
INSERT INTO afiliado (nro_afiliado, titular_id, parentesco, apellido, nombre, tipo_documento, documento, cuil, fecha_nacimiento, sexo, email, telefono, domicilio, localidad, plan_id, area_id, sector_id, derivadora_id, via_cobro, carencia, estado, fecha_alta, bloqueado, created_at, created_by)
VALUES ('131090001100', NULL, 'TITULAR', 'VEGA', 'Mariana Soledad', 'DNI', '36554210', '27-36554210-5', DATE '1992-06-19', 'F', 'mvega@example.com', '261-5332210', 'Godoy Cruz 402', 'Mendoza',
        (SELECT id FROM plan WHERE codigo='0106'), (SELECT id FROM area WHERE codigo='CIMESA'), (SELECT id FROM sector WHERE codigo='VOL-PART'), NULL, 'PMC', 'REDUCIDA', 'ACTIVO', DATE '2023-07-01', FALSE, CURRENT_TIMESTAMP, 'demo');
INSERT INTO afiliado (nro_afiliado, titular_id, parentesco, apellido, nombre, tipo_documento, documento, cuil, fecha_nacimiento, sexo, email, telefono, domicilio, localidad, plan_id, area_id, sector_id, derivadora_id, via_cobro, carencia, estado, fecha_alta, fecha_baja, motivo_baja, bloqueado, created_at, created_by)
VALUES ('131090001200', NULL, 'TITULAR', 'PAZ', 'Alberto Enrique', 'DNI', '18332904', '20-18332904-3', DATE '1963-01-27', 'M', 'apaz@example.com', '261-4110987', 'Necochea 77', 'Mendoza',
        (SELECT id FROM plan WHERE codigo='0104'), (SELECT id FROM area WHERE codigo='CIMESA'), (SELECT id FROM sector WHERE codigo='VOL-PART'), NULL, 'PMC', 'COMUN', 'BAJA', DATE '2008-10-01', DATE '2026-08-31', 'Baja por renuncia', FALSE, CURRENT_TIMESTAMP, 'demo');

-- ---------- Integrantes del grupo familiar ----------
INSERT INTO afiliado (nro_afiliado, titular_id, parentesco, apellido, nombre, tipo_documento, documento, fecha_nacimiento, sexo, plan_id, area_id, sector_id, via_cobro, carencia, estado, fecha_alta, bloqueado, created_at, created_by)
VALUES ('131090000101', (SELECT id FROM (SELECT id FROM afiliado WHERE nro_afiliado='131090000100') t), 'CONYUGE', 'PEREYRA', 'Martín Alejandro', 'DNI', '27998123', DATE '1978-08-02', 'M',
        (SELECT id FROM plan WHERE codigo='0104'), (SELECT id FROM area WHERE codigo='CIMESA'), (SELECT id FROM sector WHERE codigo='VOL-PART'), 'PMC', 'COMUN', 'ACTIVO', DATE '2015-06-01', FALSE, CURRENT_TIMESTAMP, 'demo');
INSERT INTO afiliado (nro_afiliado, titular_id, parentesco, apellido, nombre, tipo_documento, documento, fecha_nacimiento, sexo, plan_id, area_id, sector_id, via_cobro, carencia, estado, fecha_alta, bloqueado, created_at, created_by)
VALUES ('131090000102', (SELECT id FROM (SELECT id FROM afiliado WHERE nro_afiliado='131090000100') t), 'HIJO', 'PEREYRA GÓMEZ', 'Tomás', 'DNI', '52110234', DATE '2012-04-15', 'M',
        (SELECT id FROM plan WHERE codigo='0104'), (SELECT id FROM area WHERE codigo='CIMESA'), (SELECT id FROM sector WHERE codigo='VOL-PART'), 'PMC', 'COMUN', 'ACTIVO', DATE '2015-06-01', FALSE, CURRENT_TIMESTAMP, 'demo');
INSERT INTO afiliado (nro_afiliado, titular_id, parentesco, apellido, nombre, tipo_documento, documento, fecha_nacimiento, sexo, plan_id, area_id, sector_id, via_cobro, carencia, estado, fecha_alta, bloqueado, created_at, created_by)
VALUES ('131090000301', (SELECT id FROM (SELECT id FROM afiliado WHERE nro_afiliado='131090000300') t), 'HIJO', 'ROSSI', 'Valentina', 'DNI', '55663201', DATE '2016-10-30', 'F',
        (SELECT id FROM plan WHERE codigo='0101'), (SELECT id FROM area WHERE codigo='CIMESA'), (SELECT id FROM sector WHERE codigo='VOL-PART'), 'CAJA', 'COMUN', 'ACTIVO', DATE '2018-09-01', FALSE, CURRENT_TIMESTAMP, 'demo');
INSERT INTO afiliado (nro_afiliado, titular_id, parentesco, apellido, nombre, tipo_documento, documento, fecha_nacimiento, sexo, plan_id, area_id, sector_id, via_cobro, carencia, estado, fecha_alta, bloqueado, created_at, created_by)
VALUES ('131090000501', (SELECT id FROM (SELECT id FROM afiliado WHERE nro_afiliado='131090000500') t), 'CONYUGE', 'IBARRA', 'Jorge Omar', 'DNI', '24556710', DATE '1974-12-12', 'M',
        (SELECT id FROM plan WHERE codigo='0104'), (SELECT id FROM area WHERE codigo='CIMESA'), (SELECT id FROM sector WHERE codigo='VOL-PART'), 'PMC', 'COMUN', 'ACTIVO', DATE '2012-04-01', FALSE, CURRENT_TIMESTAMP, 'demo');
INSERT INTO afiliado (nro_afiliado, titular_id, parentesco, apellido, nombre, tipo_documento, documento, fecha_nacimiento, sexo, plan_id, area_id, sector_id, via_cobro, carencia, estado, fecha_alta, bloqueado, created_at, created_by)
VALUES ('131090000901', (SELECT id FROM (SELECT id FROM afiliado WHERE nro_afiliado='131090000900') t), 'HIJO', 'LUCERO NAVARRO', 'Agustina', 'DNI', '50998123', DATE '2010-09-05', 'F',
        (SELECT id FROM plan WHERE codigo='0104'), (SELECT id FROM area WHERE codigo='CIMESA'), (SELECT id FROM sector WHERE codigo='VOL-PART'), 'PMC', 'COMUN', 'ACTIVO', DATE '2014-05-01', FALSE, CURRENT_TIMESTAMP, 'demo');
INSERT INTO afiliado (nro_afiliado, titular_id, parentesco, apellido, nombre, tipo_documento, documento, fecha_nacimiento, sexo, plan_id, area_id, sector_id, via_cobro, carencia, estado, fecha_alta, bloqueado, created_at, created_by)
VALUES ('131090000902', (SELECT id FROM (SELECT id FROM afiliado WHERE nro_afiliado='131090000900') t), 'HIJO', 'LUCERO NAVARRO', 'Benjamín', 'DNI', '53887004', DATE '2013-03-22', 'M',
        (SELECT id FROM plan WHERE codigo='0104'), (SELECT id FROM area WHERE codigo='CIMESA'), (SELECT id FROM sector WHERE codigo='VOL-PART'), 'PMC', 'COMUN', 'ACTIVO', DATE '2014-05-01', FALSE, CURRENT_TIMESTAMP, 'demo');

UPDATE numerador SET ultimo = 1310900012 WHERE clave = 'AFILIADO_BASE';

-- ---------- Valores de cuota por integrante (dos vigencias para ver la actualización) ----------
INSERT INTO valor_cuota (plan_id, vigente_desde, importe, motivo, created_at, created_by) VALUES ((SELECT id FROM plan WHERE codigo='0101'), DATE '2026-08-01', 262400.00, 'Valor inicial (demo)', CURRENT_TIMESTAMP, 'demo');
INSERT INTO valor_cuota (plan_id, vigente_desde, importe, motivo, created_at, created_by) VALUES ((SELECT id FROM plan WHERE codigo='0104'), DATE '2026-08-01', 228990.00, 'Valor inicial (demo)', CURRENT_TIMESTAMP, 'demo');
INSERT INTO valor_cuota (plan_id, vigente_desde, importe, motivo, created_at, created_by) VALUES ((SELECT id FROM plan WHERE codigo='0106'), DATE '2026-08-01', 180526.00, 'Valor inicial (demo)', CURRENT_TIMESTAMP, 'demo');
INSERT INTO valor_cuota (plan_id, vigente_desde, importe, motivo, created_at, created_by) VALUES ((SELECT id FROM plan WHERE codigo='1400'), DATE '2026-08-01', 271300.00, 'Valor inicial (demo)', CURRENT_TIMESTAMP, 'demo');
INSERT INTO valor_cuota (plan_id, vigente_desde, importe, motivo, created_at, created_by) VALUES ((SELECT id FROM plan WHERE codigo='4502'), DATE '2026-08-01', 214600.00, 'Valor inicial (demo)', CURRENT_TIMESTAMP, 'demo');
INSERT INTO valor_cuota (plan_id, vigente_desde, importe, motivo, created_at, created_by) VALUES ((SELECT id FROM plan WHERE codigo='0101'), DATE '2026-10-01', 274208.00, 'Actualización 4,5% (demo)', CURRENT_TIMESTAMP, 'demo');
INSERT INTO valor_cuota (plan_id, vigente_desde, importe, motivo, created_at, created_by) VALUES ((SELECT id FROM plan WHERE codigo='0104'), DATE '2026-10-01', 239294.55, 'Actualización 4,5% (demo)', CURRENT_TIMESTAMP, 'demo');
INSERT INTO valor_cuota (plan_id, vigente_desde, importe, motivo, created_at, created_by) VALUES ((SELECT id FROM plan WHERE codigo='0106'), DATE '2026-10-01', 188649.67, 'Actualización 4,5% (demo)', CURRENT_TIMESTAMP, 'demo');
INSERT INTO valor_cuota (plan_id, vigente_desde, importe, motivo, created_at, created_by) VALUES ((SELECT id FROM plan WHERE codigo='1400'), DATE '2026-10-01', 283508.50, 'Actualización 4,5% (demo)', CURRENT_TIMESTAMP, 'demo');
INSERT INTO valor_cuota (plan_id, vigente_desde, importe, motivo, created_at, created_by) VALUES ((SELECT id FROM plan WHERE codigo='4502'), DATE '2026-10-01', 224257.00, 'Actualización 4,5% (demo)', CURRENT_TIMESTAMP, 'demo');

-- ---------- Prestadores, prácticas y valores convenidos ----------
INSERT INTO prestador (nombre, tipo, email, activo, created_at, created_by) VALUES ('Clínica Santa Rosa', 'CLINICA', 'liquidaciones@clinicasantarosa.example', TRUE, CURRENT_TIMESTAMP, 'demo');
INSERT INTO prestador (nombre, tipo, email, activo, created_at, created_by) VALUES ('Hogar Salud S.A.', 'INSTITUCION', NULL, TRUE, CURRENT_TIMESTAMP, 'demo');

INSERT INTO practica (codigo_cmm, codigo_nomenclador, descripcion, rubro, requiere_auditoria) VALUES ('420101', '42.01.01', 'Consulta', 'CONSULTA', FALSE);
INSERT INTO practica (codigo_cmm, codigo_nomenclador, descripcion, rubro, requiere_auditoria) VALUES ('420103', '42.01.02', 'Consulta de guardia', 'CONSULTA', FALSE);
INSERT INTO practica (codigo_cmm, codigo_nomenclador, descripcion, rubro, requiere_auditoria) VALUES ('429010', '42.01.03', 'Consulta pediátrica de guardia', 'CONSULTA', FALSE);
INSERT INTO practica (codigo_cmm, codigo_nomenclador, descripcion, rubro, requiere_auditoria) VALUES ('429019', '42.01.04', 'Consulta con especialista', 'CONSULTA', FALSE);
INSERT INTO practica (codigo_cmm, codigo_nomenclador, descripcion, rubro, requiere_auditoria) VALUES ('430201', '43.02.01', 'Curación plana', 'ENFERMERIA', FALSE);
INSERT INTO practica (codigo_cmm, codigo_nomenclador, descripcion, rubro, requiere_auditoria) VALUES ('430401', '43.04.01', 'Nebulización', 'ENFERMERIA', FALSE);
INSERT INTO practica (codigo_cmm, codigo_nomenclador, descripcion, rubro, requiere_auditoria) VALUES ('430603', '43.06.03', 'Toma de tensión arterial', 'ENFERMERIA', FALSE);
INSERT INTO practica (codigo_cmm, codigo_nomenclador, descripcion, rubro, requiere_auditoria) VALUES ('MED001', NULL, 'Medicación ambulatoria (provisión)', 'MEDICAMENTOS', TRUE);

INSERT INTO valor_convenido (prestador_id, practica_id, vigente_desde, importe, origen, created_at, created_by) SELECT p.id, pr.id, DATE '2026-07-01', v.importe, 'Planilla julio 2026', CURRENT_TIMESTAMP, 'demo'
FROM prestador p, practica pr, (SELECT '420101' cod, 22440.00 importe UNION ALL SELECT '420103', 25500.00 UNION ALL SELECT '429010', 27540.00 UNION ALL SELECT '429019', 27540.00
                                UNION ALL SELECT '430201', 12240.00 UNION ALL SELECT '430401', 8160.00 UNION ALL SELECT '430603', 6630.00) v
WHERE p.nombre = 'Clínica Santa Rosa' AND pr.codigo_cmm = v.cod;

INSERT INTO valor_convenido (prestador_id, practica_id, vigente_desde, importe, origen, created_at, created_by) SELECT p.id, pr.id, DATE '2026-08-01', v.importe, 'Planilla agosto 2026', CURRENT_TIMESTAMP, 'demo'
FROM prestador p, practica pr, (SELECT '420101' cod, 22911.24 importe UNION ALL SELECT '420103', 26035.50 UNION ALL SELECT '429010', 28118.34 UNION ALL SELECT '429019', 28118.34
                                UNION ALL SELECT '430201', 12497.04 UNION ALL SELECT '430401', 8331.36 UNION ALL SELECT '430603', 6769.23 UNION ALL SELECT 'MED001', 50000.00) v
WHERE p.nombre = 'Clínica Santa Rosa' AND pr.codigo_cmm = v.cod;

INSERT INTO valor_convenido (prestador_id, practica_id, vigente_desde, importe, origen, created_at, created_by) SELECT p.id, pr.id, DATE '2026-08-01', v.importe, 'Convenio 2026', CURRENT_TIMESTAMP, 'demo'
FROM prestador p, practica pr, (SELECT '420101' cod, 24000.00 importe UNION ALL SELECT '429019', 29500.00) v
WHERE p.nombre = 'Hogar Salud S.A.' AND pr.codigo_cmm = v.cod;

-- ---------- Historia clínica ----------
INSERT INTO historia_clinica_entrada (afiliado_id, fecha, tipo, profesional, diagnostico, detalle, created_at, created_by)
VALUES ((SELECT id FROM afiliado WHERE nro_afiliado='131090000800'), DATE '2026-07-29', 'CONSULTA', 'Dra. Especialista (demo)', 'Control cardiológico',
        'Consulta con especialista en cardiología. Se indica control en 6 meses y estudios de rutina.', CURRENT_TIMESTAMP, 'demo');
INSERT INTO historia_clinica_entrada (afiliado_id, fecha, tipo, profesional, diagnostico, detalle, created_at, created_by)
VALUES ((SELECT id FROM afiliado WHERE nro_afiliado='131090000800'), DATE '2026-06-12', 'CONSULTA', 'Dr. Otorrinolaringólogo (demo)', 'Consulta ORL',
        'Consulta en consultorio de nariz, garganta y oído. Sin hallazgos relevantes.', CURRENT_TIMESTAMP, 'demo');
INSERT INTO historia_clinica_entrada (afiliado_id, fecha, tipo, profesional, diagnostico, detalle, created_at, created_by)
VALUES ((SELECT id FROM afiliado WHERE nro_afiliado='131090000102'), DATE '2026-05-03', 'GUARDIA', 'Guardia pediátrica (demo)', 'Cuadro febril',
        'Consulta pediátrica de guardia por fiebre de 24 h. Se indica antitérmico y control.', CURRENT_TIMESTAMP, 'demo');
