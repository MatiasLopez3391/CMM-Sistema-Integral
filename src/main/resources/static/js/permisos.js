// Qué roles ven cada pantalla. Refleja las anotaciones @RequiereRol del backend, solo para no mostrar
// opciones que darían "Sin permiso". La autoridad es siempre el servidor: si cambia un @RequiereRol, actualizar acá.
export const ACCESO_PANTALLAS = {
  inicio: null, // sin restricción: cualquier usuario con sesión
  afiliados: ['ADMINISTRACION', 'LIQUIDACIONES', 'MESA_ENTRADAS', 'MEDICO_AUDITOR'],
  planes: ['ADMINISTRACION', 'LIQUIDACIONES', 'IT'],
  prestadores: ['ADMINISTRACION', 'LIQUIDACIONES', 'IT'],
  derivadoras: ['ADMINISTRACION', 'LIQUIDACIONES', 'IT'],
  cuotas: ['LIQUIDACIONES', 'ADMINISTRACION'],
  cobranza: ['LIQUIDACIONES', 'ADMINISTRACION'],
  morosos: ['LIQUIDACIONES', 'ADMINISTRACION'],
  contabilidad: ['LIQUIDACIONES', 'ADMINISTRACION'],
  configuracion: ['LIQUIDACIONES', 'ADMINISTRACION']
};

// Pestañas de la ficha del afiliado (null = todas las ven).
export const ACCESO_PESTANIAS = {
  datos: null,
  grupo: null,
  bloqueos: null,
  cuenta: ['LIQUIDACIONES', 'ADMINISTRACION'],
  pagos: ['LIQUIDACIONES', 'ADMINISTRACION'],
  hc: ['MEDICO_AUDITOR', 'MESA_ENTRADAS'],
  auditoria: ['ADMINISTRACION', 'IT']
};
