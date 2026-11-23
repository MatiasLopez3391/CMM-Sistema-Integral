-- Coseguros de demostración (personas ficticias): órdenes de noviembre 2026 que se cobran en la cuota de diciembre.
-- Se cargan como carga manual de Liquidaciones; cuando exista el módulo de órdenes (F5) llegarán con origen ORDEN.
INSERT INTO cargo_cuota (titular_id, afiliado_id, tipo, concepto, fecha, importe, periodo_destino, origen, origen_ref, estado, created_at, created_by)
VALUES ((SELECT id FROM afiliado WHERE nro_afiliado = '131090000100'), (SELECT id FROM afiliado WHERE nro_afiliado = '131090000101'),
        'COSEGURO', 'Consulta con especialista - Clínica Santa Rosa', DATE '2026-11-05', 5623.67, '202612', 'MANUAL', NULL, 'PENDIENTE',
        CURRENT_TIMESTAMP, 'migracion');
INSERT INTO cargo_cuota (titular_id, afiliado_id, tipo, concepto, fecha, importe, periodo_destino, origen, origen_ref, estado, created_at, created_by)
VALUES ((SELECT id FROM afiliado WHERE nro_afiliado = '131090000100'), (SELECT id FROM afiliado WHERE nro_afiliado = '131090000100'),
        'COSEGURO', 'Consulta - Clínica Santa Rosa', DATE '2026-11-12', 3480.00, '202612', 'MANUAL', NULL, 'PENDIENTE',
        CURRENT_TIMESTAMP, 'migracion');
INSERT INTO cargo_cuota (titular_id, afiliado_id, tipo, concepto, fecha, importe, periodo_destino, origen, origen_ref, estado, created_at, created_by)
VALUES ((SELECT id FROM afiliado WHERE nro_afiliado = '131090000300'), (SELECT id FROM afiliado WHERE nro_afiliado = '131090000301'),
        'COSEGURO', 'Consulta pediátrica de guardia - Hogar Salud S.A.', DATE '2026-11-18', 6150.00, '202612', 'MANUAL', NULL, 'PENDIENTE',
        CURRENT_TIMESTAMP, 'migracion');
INSERT INTO cargo_cuota (titular_id, afiliado_id, tipo, concepto, fecha, importe, periodo_destino, origen, origen_ref, estado, created_at, created_by)
VALUES ((SELECT id FROM afiliado WHERE nro_afiliado = '131090000900'), (SELECT id FROM afiliado WHERE nro_afiliado = '131090000902'),
        'COSEGURO', 'Nebulización - Hogar Salud S.A.', DATE '2026-11-20', 1890.00, '202612', 'MANUAL', NULL, 'PENDIENTE',
        CURRENT_TIMESTAMP, 'migracion');
