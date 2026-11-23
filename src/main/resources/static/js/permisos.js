// Qué roles ven cada pantalla. Refleja las anotaciones @RequiereRol del backend, solo para no mostrar
// opciones que darían "Sin permiso". La autoridad es siempre el servidor: si cambia un @RequiereRol, actualizar acá.
export const ACCESO_PANTALLAS = {
  inicio: null, // sin restricción: cualquier usuario con sesión
  afiliados: ['ADMINISTRACION', 'LIQUIDACIONES', 'MESA_ENTRADAS', 'MEDICO_AUDITOR'],
  // Mesa de Entradas (emisora de órdenes) consulta planes, prestadores y coseguros, sin modificarlos
  planes: ['ADMINISTRACION', 'LIQUIDACIONES', 'MESA_ENTRADAS', 'IT'],
  prestadores: ['ADMINISTRACION', 'LIQUIDACIONES', 'MESA_ENTRADAS', 'IT'],
  derivadoras: ['ADMINISTRACION', 'LIQUIDACIONES', 'MESA_ENTRADAS', 'IT'],
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
  hc: null, // la ven todos; cargar entradas es otra acción (ver ACCIONES)
  auditoria: ['ADMINISTRACION', 'IT', 'MEDICO_AUDITOR']
};

// Acciones de edición dentro de la ficha del afiliado (el médico auditor solo consulta).
export const ACCIONES = {
  editarAfiliado: ['ADMINISTRACION', 'LIQUIDACIONES', 'MESA_ENTRADAS'], // alta, modificación, integrantes y baja
  bloquearAfiliado: ['ADMINISTRACION', 'LIQUIDACIONES'],
  cargarHistoriaClinica: ['MESA_ENTRADAS'],
  editarCobertura: ['ADMINISTRACION', 'LIQUIDACIONES', 'IT'] // planes, derivadoras, prestadores, prácticas y valores
};
