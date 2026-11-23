-- Perfiles de los usuarios de demostración (T36):
--   silvina  -> permiso total (todos los roles)
--   gisela   -> Facturación y Cobranza (+ datos del afiliado)
--   daniela  -> emisora de órdenes (+ datos del afiliado)
--   ludovico -> médico auditor: solo consulta de datos del paciente
-- Se hace en una migración nueva porque V952 ya se aplicó en algunas bases (nunca se edita una migración subida).
UPDATE usuario SET nombre = 'Silvina'        WHERE username = 'silvina';
UPDATE usuario SET nombre = 'Gisela'         WHERE username = 'gisela';
UPDATE usuario SET nombre = 'Daniela'        WHERE username = 'daniela';
UPDATE usuario SET nombre = 'Médico Auditor' WHERE username = 'ludovico';

DELETE FROM usuario_rol WHERE usuario_id IN (SELECT id FROM usuario WHERE username IN ('silvina', 'gisela', 'daniela', 'ludovico'));

INSERT INTO usuario_rol (usuario_id, rol) SELECT id, 'ADMINISTRACION' FROM usuario WHERE username = 'silvina';
INSERT INTO usuario_rol (usuario_id, rol) SELECT id, 'LIQUIDACIONES'  FROM usuario WHERE username = 'silvina';
INSERT INTO usuario_rol (usuario_id, rol) SELECT id, 'MESA_ENTRADAS'  FROM usuario WHERE username = 'silvina';
INSERT INTO usuario_rol (usuario_id, rol) SELECT id, 'MEDICO_AUDITOR' FROM usuario WHERE username = 'silvina';
INSERT INTO usuario_rol (usuario_id, rol) SELECT id, 'IT'             FROM usuario WHERE username = 'silvina';
INSERT INTO usuario_rol (usuario_id, rol) SELECT id, 'LIQUIDACIONES'  FROM usuario WHERE username = 'gisela';
INSERT INTO usuario_rol (usuario_id, rol) SELECT id, 'MESA_ENTRADAS'  FROM usuario WHERE username = 'daniela';
INSERT INTO usuario_rol (usuario_id, rol) SELECT id, 'MEDICO_AUDITOR' FROM usuario WHERE username = 'ludovico';
