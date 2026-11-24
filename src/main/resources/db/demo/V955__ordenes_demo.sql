-- Órdenes de demostración (personas ficticias): dos pedidos de medicación esperando al médico auditor.
-- Las órdenes pendientes no generan coseguro: se registra en la cuota recién cuando se autorizan.
INSERT INTO orden (numero, afiliado_id, prestador_id, plan_id, fecha_emision, estado, total_valor_convenido, total_coseguro,
                   total_cargo_entidad, emitida_por, motivo_auditoria, medico_solicitante, observaciones, created_at, created_by)
VALUES ('ORD-00000001', (SELECT id FROM afiliado WHERE nro_afiliado = '131090000500'),
        (SELECT id FROM prestador WHERE nombre = 'Clínica Santa Rosa'), (SELECT id FROM plan WHERE codigo = '0104'),
        DATE '2026-11-20', 'PENDIENTE_AUDITORIA', 100000.00, 20000.00, 80000.00, 'daniela',
        'La práctica Medicación ambulatoria (provisión) requiere auditoría', 'Dra. Sosa (MP 1234)',
        'Tratamiento de dos meses', CURRENT_TIMESTAMP, 'daniela');
INSERT INTO orden (numero, afiliado_id, prestador_id, plan_id, fecha_emision, estado, total_valor_convenido, total_coseguro,
                   total_cargo_entidad, emitida_por, motivo_auditoria, medico_solicitante, observaciones, created_at, created_by)
VALUES ('ORD-00000002', (SELECT id FROM afiliado WHERE nro_afiliado = '131090001100'),
        (SELECT id FROM prestador WHERE nombre = 'Clínica Santa Rosa'), (SELECT id FROM plan WHERE codigo = '0106'),
        DATE '2026-11-22', 'PENDIENTE_AUDITORIA', 50000.00, 15000.00, 35000.00, 'daniela',
        'La práctica Medicación ambulatoria (provisión) requiere auditoría', 'Dr. Funes (MP 5678)',
        NULL, CURRENT_TIMESTAMP, 'daniela');

INSERT INTO orden_item (orden_id, practica_id, cantidad, cantidad_solicitada, valor_convenido, porcentaje_cobertura,
                        cargo_entidad, coseguro_afiliado, requiere_auditoria)
VALUES ((SELECT id FROM orden WHERE numero = 'ORD-00000001'), (SELECT id FROM practica WHERE codigo_cmm = 'MED001'),
        2, 2, 50000.00, 80.0000, 80000.00, 20000.00, TRUE);
INSERT INTO orden_item (orden_id, practica_id, cantidad, cantidad_solicitada, valor_convenido, porcentaje_cobertura,
                        cargo_entidad, coseguro_afiliado, requiere_auditoria)
VALUES ((SELECT id FROM orden WHERE numero = 'ORD-00000002'), (SELECT id FROM practica WHERE codigo_cmm = 'MED001'),
        1, 1, 50000.00, 70.0000, 35000.00, 15000.00, TRUE);

INSERT INTO orden_estado_historial (orden_id, estado_anterior, estado_nuevo, fecha, usuario, comentario)
VALUES ((SELECT id FROM orden WHERE numero = 'ORD-00000001'), NULL, 'PENDIENTE_AUDITORIA', TIMESTAMP '2026-11-20 10:15:00', 'daniela',
        'Emitida; va a auditoría: La práctica Medicación ambulatoria (provisión) requiere auditoría');
INSERT INTO orden_estado_historial (orden_id, estado_anterior, estado_nuevo, fecha, usuario, comentario)
VALUES ((SELECT id FROM orden WHERE numero = 'ORD-00000002'), NULL, 'PENDIENTE_AUDITORIA', TIMESTAMP '2026-11-22 11:40:00', 'daniela',
        'Emitida; va a auditoría: La práctica Medicación ambulatoria (provisión) requiere auditoría');

UPDATE numerador SET ultimo = 2 WHERE clave = 'ORDEN';
