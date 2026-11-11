-- Textos de los datos de demostración con apariencia de sistema en producción.
UPDATE valor_cuota SET motivo = 'Valor inicial' WHERE motivo = 'Valor inicial (demo)';
UPDATE valor_cuota SET motivo = 'Actualización 4,5%' WHERE motivo = 'Actualización 4,5% (demo)';
UPDATE historia_clinica_entrada SET profesional = 'Servicio de Cardiología' WHERE profesional = 'Dra. Especialista (demo)';
UPDATE historia_clinica_entrada SET profesional = 'Servicio de Otorrinolaringología' WHERE profesional = 'Dr. Otorrinolaringólogo (demo)';
UPDATE historia_clinica_entrada SET profesional = 'Guardia pediátrica' WHERE profesional = 'Guardia pediátrica (demo)';
UPDATE bloqueo_historial SET motivo = REPLACE(motivo, ' (demo)', '');
UPDATE auditoria_cambio SET detalle = REPLACE(detalle, ' (demo)', '');
UPDATE afiliado SET created_by = 'migracion' WHERE created_by = 'demo';
UPDATE valor_cuota SET created_by = 'migracion' WHERE created_by = 'demo';
UPDATE prestador SET created_by = 'migracion' WHERE created_by = 'demo';
UPDATE valor_convenido SET created_by = 'migracion' WHERE created_by = 'demo';
UPDATE historia_clinica_entrada SET created_by = 'migracion' WHERE created_by = 'demo';
UPDATE prestador SET email = 'liquidaciones@clinicasantarosa.com.ar' WHERE email = 'liquidaciones@clinicasantarosa.example';
