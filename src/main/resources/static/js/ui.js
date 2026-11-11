// Utilidades de interfaz compartidas por todas las vistas.

export function esc(v) {
  if (v === null || v === undefined) return '';
  return String(v).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

/** Plantilla segura: los valores interpolados se escapan salvo que vengan envueltos con raw(). */
export function html(partes, ...valores) {
  return partes.reduce((acc, p, i) => {
    let v = i < valores.length ? valores[i] : '';
    if (Array.isArray(v)) v = v.map(x => (x && x.__raw !== undefined) ? x.__raw : esc(x)).join('');
    else if (v && v.__raw !== undefined) v = v.__raw;
    else v = esc(v);
    return acc + p + v;
  }, '');
}

export const raw = s => ({ __raw: s ?? '' });

export function $(sel, raiz = document) { return raiz.querySelector(sel); }
export function $$(sel, raiz = document) { return [...raiz.querySelectorAll(sel)]; }

export function toast(msg, tipo = '') {
  const t = document.createElement('div');
  t.className = `toast ${tipo}`;
  t.textContent = msg;
  $('#toasts').appendChild(t);
  setTimeout(() => t.remove(), tipo === 'error' ? 7000 : 4000);
}

export function error(e) {
  console.error(e);
  toast(e.message || 'Error inesperado', 'error');
}

/** Abre un modal. contenido: HTML. Devuelve el elemento .modal y una función cerrar. */
export function modal(titulo, contenido) {
  const fondo = document.createElement('div');
  fondo.className = 'modal-fondo';
  fondo.innerHTML = `<div class="modal" role="dialog" aria-modal="true"><h2>${esc(titulo)}</h2>${contenido}</div>`;
  document.body.appendChild(fondo);
  const cerrar = () => fondo.remove();
  fondo.addEventListener('click', e => { if (e.target === fondo) cerrar(); });
  fondo.querySelectorAll('[data-cerrar]').forEach(b => b.addEventListener('click', cerrar));
  const primero = fondo.querySelector('input, select, textarea');
  if (primero) setTimeout(() => primero.focus(), 30);
  return { el: fondo.querySelector('.modal'), cerrar };
}

/** Serializa un formulario a objeto. Campos vacíos -> null; data-tipo="num" -> número; checkbox -> boolean. */
export function datosForm(form) {
  const o = {};
  $$('input, select, textarea', form).forEach(el => {
    if (!el.name) return;
    if (el.type === 'checkbox') { o[el.name] = el.checked; return; }
    if (el.type === 'file') return;
    let v = el.value.trim();
    if (v === '') v = null;
    else if (el.dataset.tipo === 'num') v = Number(v.replace(',', '.'));
    o[el.name] = v;
  });
  return o;
}

export function badgeEstado(estado) {
  const mapa = {
    ACTIVO: 'ok', BAJA: '', PAGADO: 'ok', PENDIENTE: 'aviso', PARCIAL: 'aviso', ANULADO: '',
    IMPUTADO: 'ok', RECHAZADO: 'error', CONFIRMADO: 'ok', PREVISUALIZADO: 'info', DESCARTADO: '',
    ENVIADA: 'ok', SIN_SMTP: 'aviso', SIN_EMAIL: 'aviso', ERROR: 'error', BLOQUEO: 'error', DESBLOQUEO: 'ok'
  };
  return `<span class="badge ${mapa[estado] ?? ''}">${esc(estado)}</span>`;
}

export const badgeBloqueado = b => b ? '<span class="badge error">Bloqueado</span>' : '';

/** Tabla genérica. columnas: [{t: título, v: fila => html, cls}] */
export function tabla(columnas, filas, { vacio = 'Sin datos', clic = false, pie = null } = {}) {
  if (!filas || filas.length === 0) return `<div class="tabla-cont"><div class="vacio">${esc(vacio)}</div></div>`;
  const cab = columnas.map(c => `<th class="${c.cls || ''}">${esc(c.t)}</th>`).join('');
  const cuerpo = filas.map((f, i) =>
    `<tr data-i="${i}" class="${clic ? 'clic' : ''}">${columnas.map(c => `<td class="${c.cls || ''}">${c.v(f)}</td>`).join('')}</tr>`
  ).join('');
  const tpie = pie ? `<tfoot><tr>${pie.map(p => `<td class="${p.cls || ''}">${p.v ?? ''}</td>`).join('')}</tr></tfoot>` : '';
  return `<div class="tabla-cont"><table><thead><tr>${cab}</tr></thead><tbody>${cuerpo}</tbody>${tpie}</table></div>`;
}

export function opciones(lista, valorSel, { valor = 'id', texto = 'nombre', vacio = null } = {}) {
  const ops = vacio !== null ? [`<option value="">${esc(vacio)}</option>`] : [];
  lista.forEach(x => {
    const v = typeof valor === 'function' ? valor(x) : x[valor];
    const t = typeof texto === 'function' ? texto(x) : x[texto];
    ops.push(`<option value="${esc(v)}" ${String(v) === String(valorSel ?? '') ? 'selected' : ''}>${esc(t)}</option>`);
  });
  return ops.join('');
}

export function cargando(el) {
  el.innerHTML = '<div class="tarjeta"><div class="vacio">Cargando…</div></div>';
}

/** Deshabilita un botón mientras corre la promesa. */
export async function conBoton(btn, fn) {
  const txt = btn.textContent;
  btn.disabled = true;
  btn.textContent = 'Procesando…';
  try { return await fn(); } finally { btn.disabled = false; btn.textContent = txt; }
}
