-- Usuarios de DEMOSTRACIÓN (T36). Contraseña = usuario + año (ej. silvina2026), guardada con BCrypt.
-- Esta carpeta (db/demo) NO se aplica en producción: allí los usuarios los carga el CMM.
INSERT INTO usuario (username, nombre, clave_hash, activo) VALUES
 ('silvina',  'Silvina (Contadora)',               '$2a$10$mTQTL/xg3FLNG99Vjbz5Ree0EpfhKRCLWlVFj6XRHFBUFwSvkkRla', TRUE),
 ('daniela',  'Daniela (Afiliaciones)',            '$2a$10$ksTPBjNYXBHlmrkWcwh4w.QIs3xgZe6iS8CPw66XwPDFKXfntcVAm', TRUE),
 ('gisela',   'Gisela (Facturación y Cobranza)',   '$2a$10$GGRPP/AowJzjQc86XYnlpeGECtPGnkIkVAQnX7Av9aN7DkHKhGyLW', TRUE),
 ('ludovico', 'Ludovico (Auditor)',                '$2a$10$9w16jdSrXGpguF3Mox/hj.Ax2Y0r2h/wDh2sL8RebnagFzaeoUmRu', TRUE);

INSERT INTO usuario_rol (usuario_id, rol) SELECT id, 'LIQUIDACIONES'  FROM usuario WHERE username = 'silvina';
INSERT INTO usuario_rol (usuario_id, rol) SELECT id, 'ADMINISTRACION' FROM usuario WHERE username = 'daniela';
INSERT INTO usuario_rol (usuario_id, rol) SELECT id, 'LIQUIDACIONES'  FROM usuario WHERE username = 'gisela';
INSERT INTO usuario_rol (usuario_id, rol) SELECT id, 'MEDICO_AUDITOR' FROM usuario WHERE username = 'ludovico';
