-- Limpieza de textos de catálogos que se muestran en pantalla.
-- Los datos que el CMM debe confirmar quedan documentados en el README (sección 5).
UPDATE cuenta_contable SET nombre = REPLACE(nombre, ' (A CONFIRMAR)', '');
UPDATE derivadora SET convenio = NULL WHERE convenio = 'A CONFIRMAR';
UPDATE layout_archivo SET observaciones = NULL;
