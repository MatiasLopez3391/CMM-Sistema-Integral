// Órdenes médicas (F5): emisión para Mesa de Entradas, consulta, detalle, anulación e impresión.
import { api, fmt, tieneRol } from '../api.js';
import { html, raw, esc, $, $$, tabla, modal, toast, error, opciones, cargando, conBoton, badgeBloqueado } from '../ui.js';

export const ETIQUETAS_ESTADO = {
  EMITIDA: ['Emitida', 'ok'],
  PENDIENTE_AUDITORIA: ['En auditoría', 'aviso'],
  AUTORIZADA: ['Autorizada', 'ok'],
  RECHAZADA: ['Rechazada', 'error'],
  ANULADA: ['Anulada', '']
};

export function badgeOrden(o) {
  const [t, cls] = ETIQUETAS_ESTADO[o.estado] || [o.estado, ''];
  return `<span class="badge ${cls}">${esc(t)}</span>${o.vencida ? ' <span class="badge">Vencida</span>' : ''}`;
}

const DECISIONES = { AUTORIZADA: 'Autorizada', AUTORIZADA_PARCIAL: 'Autorizada con cambios', RECHAZADA: 'Rechazada' };
const puedeEmitir = () => tieneRol(['MESA_ENTRADAS']);
const puedeAnular = () => tieneRol(['MESA_ENTRADAS', 'ADMINISTRACION']);
const recibeAvisos = () => tieneRol(['MESA_ENTRADAS', 'ADMINISTRACION']);

export async function render(el, params) {
  const pestanias = [
    ...(puedeEmitir() ? [['emitir', 'Emitir orden']] : []),
    ['consultar', 'Consultar órdenes'],
    ...(recibeAvisos() ? [['resueltas', 'Resueltas por auditoría']] : [])
  ];
  const tab = pestanias.some(([k]) => k === params[0]) ? params[0] : pestanias[0][0];
  el.innerHTML = html`
    <div class="tarjeta" style="padding-bottom:0">
      <div class="tabs" id="tabs">
        ${raw(pestanias.map(([k, t]) => `<button data-tab="${k}" class="${k === tab ? 'activo' : ''}">${t}</button>`).join(''))}
      </div>
    </div>
    <div id="contenido"></div>`;
  $$('#tabs button', el).forEach(b => b.addEventListener('click', () => { location.hash = `#/ordenes/${b.dataset.tab}`; }));
  const c = $('#contenido', el);
  cargando(c);
  try {
    if (tab === 'emitir') await tabEmitir(c, params[1]);
    else if (tab === 'resueltas') { window.dispatchEvent(new Event('cmm:avisos-vistos')); await tabResueltas(c); }
    else await tabConsultar(c);
  } catch (e) { error(e); }
}

// ---------------------------------------------------------------- Emisión
async function tabEmitir(c, afiliadoInicial) {
  const [prestadores, practicas] = await Promise.all([api.get('/api/cobertura/prestadores'), api.get('/api/cobertura/practicas')]);
  const activos = prestadores.filter(p => p.activo);
  let afiliado = null;
  let valores = [];

  c.innerHTML = html`
    <div class="tarjeta">
      <h2>Nueva orden</h2>
      <div class="form-grid">
        <div class="campo" style="grid-column:1/-1"><label>Afiliado (número, apellido o documento) *</label>
          <div class="fila"><input id="qAf" placeholder="Ej.: 131090000100 o Gómez" style="flex:1"><button type="button" class="btn" id="bAf">Buscar</button></div>
          <div id="resAf" style="margin-top:6px"></div>
          <div id="selAf" style="margin-top:6px"></div></div>
        <div class="campo"><label>Prestador *</label><select id="prest">${raw(opciones(activos, null, { vacio: 'Elija un prestador' }))}</select></div>
        <div class="campo"><label>Médico que prescribe</label><input id="medico" maxlength="150" placeholder="Ej.: Dra. Sosa (MP 1234)"></div>
        <div class="campo" style="grid-column:1/-1"><label>Indicación u observaciones</label><textarea id="obs" maxlength="1000" placeholder="Diagnóstico o indicación del médico"></textarea></div>
      </div>
      <h3 style="margin-top:14px">Prácticas</h3>
      <div id="filas"></div>
      <button type="button" class="btn chico" id="agregar" style="margin-top:8px">+ Agregar práctica</button>
      <div style="margin-top:14px">
        <label style="display:inline-flex;gap:6px"><input type="checkbox" id="pideAud"> El médico pide que la orden pase por auditoría</label>
        <div class="campo" id="campoMotivo" hidden style="margin-top:6px"><label>Motivo del pedido *</label><input id="motivoAud" maxlength="250"></div>
      </div>
      <div id="resultado" style="margin-top:14px"></div>
      <div class="acciones">
        <button type="button" class="btn" id="calcular">Calcular coseguro</button>
        <button type="button" class="btn primario" id="emitir">Emitir orden</button>
      </div>
    </div>`;

  const filas = $('#filas', c);
  const opcionesPractica = () => {
    if (!valores.length) return '<option value="">Elija primero el prestador</option>';
    return '<option value="">Elija una práctica</option>' + valores.map(v => {
      const p = practicas.find(x => x.id === v.practicaId);
      const aud = p?.requiereAuditoria ? ' · requiere auditoría' : '';
      return `<option value="${v.practicaId}">${esc(v.codigoCmm)} · ${esc(v.descripcion)} (${fmt.moneda(v.importe)})${aud}</option>`;
    }).join('');
  };
  const agregarFila = () => {
    const div = document.createElement('div');
    div.className = 'fila';
    div.style.marginBottom = '6px';
    div.innerHTML = `<select class="selPrac" style="flex:1">${opcionesPractica()}</select>
      <input class="cant" type="number" min="1" value="1" style="width:80px" aria-label="Cantidad">
      <button type="button" class="btn chico quitar" title="Quitar práctica">Quitar</button>`;
    div.querySelector('.quitar').addEventListener('click', () => { div.remove(); $('#resultado', c).innerHTML = ''; });
    div.querySelectorAll('select, input').forEach(x => x.addEventListener('change', () => { $('#resultado', c).innerHTML = ''; }));
    filas.appendChild(div);
  };
  agregarFila();
  $('#agregar', c).addEventListener('click', agregarFila);

  $('#prest', c).addEventListener('change', async e => {
    valores = [];
    if (e.target.value) {
      try { valores = await api.get(`/api/cobertura/prestadores/${e.target.value}/valores`); } catch (er) { error(er); }
    }
    $$('.selPrac', c).forEach(s => { s.innerHTML = opcionesPractica(); });
    $('#resultado', c).innerHTML = '';
  });
  $('#pideAud', c).addEventListener('change', e => { $('#campoMotivo', c).hidden = !e.target.checked; });

  const elegir = a => {
    afiliado = a;
    $('#resAf', c).innerHTML = '';
    $('#selAf', c).innerHTML = a ? `<div class="cinta ${a.bloqueado || a.estado !== 'ACTIVO' ? 'error' : 'info'}" style="margin:0">
      <b>${esc(a.apellidoNombre)}</b> · <span class="mono">${esc(a.nroAfiliado)}</span> · ${esc(a.parentesco)} · Plan ${esc(a.planCodigo)} ${badgeBloqueado(a.bloqueado)}
      ${a.bloqueado ? '<br>El grupo familiar está bloqueado: no se pueden emitir órdenes hasta que se lo desbloquee.' : ''}
      ${a.estado !== 'ACTIVO' ? '<br>El afiliado no está activo.' : ''}
      <a href="#/afiliados/${a.id}/ordenes" style="margin-left:8px">Ver sus órdenes</a></div>` : '';
    $('#resultado', c).innerHTML = '';
  };
  const buscar = async () => {
    try {
      const r = await api.get('/api/afiliados', { q: $('#qAf', c).value, tamanio: 8 });
      $('#resAf', c).innerHTML = tabla([
        { t: 'Nro. afiliado', v: a => `<span class="mono">${esc(a.nroAfiliado)}</span>` },
        { t: 'Apellido y nombre', v: a => esc(a.apellidoNombre) + ' ' + badgeBloqueado(a.bloqueado) },
        { t: 'Parentesco', v: a => esc(a.parentesco) },
        { t: 'Plan', v: a => esc(a.planCodigo) },
        { t: 'Estado', v: a => esc(a.estado) },
        { t: '', cls: 'derecha', v: (a, i) => `<button type="button" class="btn chico" data-elegir="${a.id}">Elegir</button>` }
      ], r.contenido, { vacio: 'Sin resultados.' });
      $$('[data-elegir]', c).forEach(b => b.addEventListener('click', () => elegir(r.contenido.find(a => String(a.id) === b.dataset.elegir))));
    } catch (e) { error(e); }
  };
  $('#bAf', c).addEventListener('click', buscar);
  $('#qAf', c).addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); buscar(); } });
  if (afiliadoInicial) {
    try {
      const f = await api.get(`/api/afiliados/${afiliadoInicial}`);
      elegir({ id: f.id, nroAfiliado: f.nroAfiliado, apellidoNombre: `${f.apellido}, ${f.nombre}`, parentesco: f.parentesco,
        planCodigo: f.planCodigo, estado: f.estado, bloqueado: f.bloqueado });
    } catch (e) { error(e); }
  }

  const pedido = () => {
    if (!afiliado) throw new Error('Elija el afiliado.');
    const prestadorId = Number($('#prest', c).value);
    if (!prestadorId) throw new Error('Elija el prestador.');
    const items = $$('#filas .fila', c).map(f => ({ practicaId: Number(f.querySelector('.selPrac').value), cantidad: Number(f.querySelector('.cant').value) }))
      .filter(i => i.practicaId);
    if (!items.length) throw new Error('Agregue al menos una práctica.');
    return {
      afiliadoId: afiliado.id, prestadorId, items,
      medicoSolicitante: $('#medico', c).value.trim() || null,
      observaciones: $('#obs', c).value.trim() || null,
      enviarAAuditoria: $('#pideAud', c).checked,
      motivoAuditoria: $('#motivoAud', c).value.trim() || null
    };
  };

  const mostrarSimulacion = s => {
    $('#resultado', c).innerHTML = `
      ${s.vaAAuditoria
        ? `<div class="cinta aviso">Esta orden va a <b>auditoría médica</b>: ${esc(s.motivoAuditoria)}. El coseguro se cobra recién cuando el auditor la autorice.</div>`
        : `<div class="cinta info">La orden se emite en el momento. El coseguro se cobra en la cuota del mes siguiente.</div>`}
      ${tabla([
        { t: 'Código', v: i => `<span class="mono">${esc(i.codigo)}</span>` },
        { t: 'Práctica', v: i => esc(i.descripcion) + (i.requiereAuditoria ? ' <span class="badge aviso">Requiere auditoría</span>' : '') },
        { t: 'Cant.', cls: 'num', v: i => i.cantidad },
        { t: 'Valor unitario', cls: 'num', v: i => fmt.moneda(i.valorUnitario) },
        { t: 'Cobertura', cls: 'num', v: i => fmt.pct(i.porcentajeCobertura) },
        { t: 'A cargo del CMM', cls: 'num', v: i => fmt.moneda(i.cargoEntidad) },
        { t: 'Coseguro', cls: 'num', v: i => `<b>${fmt.moneda(i.coseguro)}</b>` }
      ], s.items, { pie: [{}, { v: 'Total' }, {}, {}, {}, { cls: 'num', v: fmt.moneda(s.totalCargoEntidad) }, { cls: 'num', v: `<b>${fmt.moneda(s.totalCoseguro)}</b>` }] })}
      <p class="ayuda">Órdenes del afiliado en el mes: ${s.ordenesEnElMes} (tope ${s.topeMensual}).</p>`;
  };

  $('#calcular', c).addEventListener('click', e => conBoton(e.target, async () => {
    try { mostrarSimulacion(await api.post('/api/ordenes/simular', pedido())); } catch (er) { error(er); }
  }));
  $('#emitir', c).addEventListener('click', e => conBoton(e.target, async () => {
    try {
      const o = await api.post('/api/ordenes', pedido());
      const pendiente = o.estado === 'PENDIENTE_AUDITORIA';
      toast(pendiente ? `Orden ${o.numero} enviada a auditoría` : `Orden ${o.numero} emitida`, 'ok');
      c.innerHTML = html`
        <div class="tarjeta">
          <h2>Orden ${o.numero}</h2>
          ${raw(pendiente
            ? '<div class="cinta aviso">La orden quedó <b>pendiente de auditoría</b>. Le avisamos en esta pantalla cuando el médico auditor la resuelva.</div>'
            : `<div class="cinta ok">Orden emitida. Coseguro a cargo del afiliado: <b>${fmt.moneda(o.totalCoseguro)}</b>, que se cobra en la cuota del mes siguiente.</div>`)}
          <div class="acciones">
            <button class="btn" id="verDet">Ver detalle</button>
            ${raw(pendiente ? '' : '<button class="btn" id="imprimir">Imprimir orden</button>')}
            <a class="btn primario" href="#/ordenes/emitir" id="otra">Emitir otra orden</a>
          </div>
        </div>`;
      $('#verDet', c).addEventListener('click', () => verOrden(o.id));
      $('#imprimir', c)?.addEventListener('click', ev => conBoton(ev.target, () => imprimir(o.id)));
      $('#otra', c).addEventListener('click', ev => {
        ev.preventDefault();
        if (location.hash === '#/ordenes/emitir') render(c.parentElement, ['emitir']); else location.hash = '#/ordenes/emitir';
      });
    } catch (er) { error(er); }
  }));
}

async function imprimir(id) {
  try { await api.descargar(`/api/ordenes/${id}/imprimir`); } catch (e) { error(e); }
}

// ---------------------------------------------------------------- Consulta
async function tabConsultar(c) {
  const hoy = fmt.hoy();
  const desdeDefecto = `${hoy.substring(0, 8)}01`;
  c.innerHTML = html`
    <div class="tarjeta">
      <div class="tarjeta-titulo"><h2>Órdenes</h2>
        <div class="fila">
          <div class="campo chico"><label>Número</label><input id="fNum" placeholder="ORD-…"></div>
          <div class="campo chico"><label>Estado</label><select id="fEst">
            <option value="">Todos</option>${raw(Object.entries(ETIQUETAS_ESTADO).map(([k, [t]]) => `<option value="${k}">${t}</option>`).join(''))}</select></div>
          <div class="campo chico"><label>Desde</label><input type="date" id="fDesde" value="${desdeDefecto}"></div>
          <div class="campo chico"><label>Hasta</label><input type="date" id="fHasta" value="${hoy}"></div>
          <button class="btn" id="fBuscar" style="align-self:flex-end">Buscar</button>
        </div>
      </div>
      <div id="lista"></div>
    </div>`;
  const cargar = async () => {
    const lista = await api.get('/api/ordenes', { numero: $('#fNum', c).value, estado: $('#fEst', c).value, desde: $('#fDesde', c).value, hasta: $('#fHasta', c).value });
    $('#lista', c).innerHTML = tablaOrdenes(lista, 'No hay órdenes con esos filtros.');
    enlazarFilas($('#lista', c), lista, cargar);
  };
  $('#fBuscar', c).addEventListener('click', () => cargar().catch(error));
  await cargar();
}

export function tablaOrdenes(lista, vacio, { conAfiliado = true } = {}) {
  return tabla([
    { t: 'Número', v: o => `<span class="mono" style="white-space:nowrap">${esc(o.numero)}</span>` },
    { t: 'Emisión', v: o => fmt.fecha(o.fechaEmision) },
    ...(conAfiliado ? [{ t: 'Afiliado', v: o => `${esc(o.afiliado)} <span class="suave mono">${esc(o.nroAfiliado)}</span>` }] : []),
    { t: 'Prestador', v: o => esc(o.prestador) },
    { t: 'Estado', v: o => badgeOrden(o) },
    { t: 'Coseguro', cls: 'num', v: o => fmt.moneda(o.totalCoseguro) },
    { t: 'Vence', v: o => fmt.fecha(o.fechaVencimiento) }
  ], lista, { vacio, clic: true });
}

export function enlazarFilas(cont, lista, alCambiar) {
  $$('tbody tr', cont).forEach(tr => tr.addEventListener('click', () => verOrden(lista[Number(tr.dataset.i)].id, { alCambiar })));
}

// ---------------------------------------------------------------- Resueltas
async function tabResueltas(c) {
  const desde = new Date(Date.now() - 7 * 864e5 - new Date().getTimezoneOffset() * 60000).toISOString().substring(0, 19);
  const avisos = (await api.get('/api/ordenes/avisos', { desde })).reverse();
  c.innerHTML = html`
    <div class="tarjeta">
      <h2>Resueltas por auditoría (últimos 7 días)</h2>
      <div class="cinta info">Cuando el médico auditor resuelve una orden, el sistema avisa en pantalla a Mesa de Entradas en menos de un minuto.</div>
      ${raw(tabla([
        { t: 'Número', v: a => `<span class="mono" style="white-space:nowrap">${esc(a.numero)}</span>` },
        { t: 'Afiliado', v: a => esc(a.afiliado) },
        { t: 'Resultado', v: a => badgeOrden(a) },
        { t: 'Resuelta', v: a => fmt.fechaHora(a.fechaResolucion) },
        { t: 'Comentario del auditor', v: a => esc(a.comentario || '—') }
      ], avisos, { vacio: 'No hubo órdenes resueltas en los últimos 7 días.', clic: true }))}
    </div>`;
  $$('tbody tr', c).forEach(tr => tr.addEventListener('click', () => verOrden(avisos[Number(tr.dataset.i)].id, { alCambiar: () => tabResueltas(c) })));
}

// ---------------------------------------------------------------- Detalle
export async function verOrden(id, { alCambiar } = {}) {
  let d;
  try { d = await api.get(`/api/ordenes/${id}`); } catch (e) { error(e); return; }
  const o = d.orden;
  const anulable = ['EMITIDA', 'AUTORIZADA', 'PENDIENTE_AUDITORIA'].includes(o.estado) && !o.vencida && puedeAnular();
  const imprimible = ['EMITIDA', 'AUTORIZADA'].includes(o.estado);
  const m = modal(`Orden ${o.numero}`, `
    <div class="datos">
      <div><div class="dato-etiqueta">Afiliado</div><div class="dato-valor"><a href="#/afiliados/${o.afiliadoId}/ordenes">${esc(o.afiliado)}</a> <span class="suave mono">${esc(o.nroAfiliado)}</span></div></div>
      <div><div class="dato-etiqueta">Plan</div><div class="dato-valor">${esc(d.planCodigo)}</div></div>
      <div><div class="dato-etiqueta">Prestador</div><div class="dato-valor">${esc(o.prestador)}</div></div>
      <div><div class="dato-etiqueta">Estado</div><div class="dato-valor">${badgeOrden(o)}</div></div>
      <div><div class="dato-etiqueta">Emisión</div><div class="dato-valor">${fmt.fecha(o.fechaEmision)} <span class="suave">por ${esc(d.emitidaPor)}</span></div></div>
      <div><div class="dato-etiqueta">Vence</div><div class="dato-valor">${fmt.fecha(o.fechaVencimiento)}</div></div>
      <div><div class="dato-etiqueta">Médico que prescribe</div><div class="dato-valor">${esc(d.medicoSolicitante || '—')}</div></div>
    </div>
    ${d.observaciones ? `<p style="margin-top:10px"><b>Indicación:</b> ${esc(d.observaciones)}</p>` : ''}
    ${o.motivoAuditoria ? `<div class="cinta aviso" style="margin-top:10px">Fue a auditoría: ${esc(o.motivoAuditoria)}</div>` : ''}
    <h3 style="margin-top:12px">Prácticas</h3>
    ${tablaItems(d)}
    ${d.auditorias.length ? `<h3 style="margin-top:12px">Auditoría médica</h3>${tabla([
      { t: 'Fecha', v: a => fmt.fechaHora(a.fecha) },
      { t: 'Decisión', v: a => esc(DECISIONES[a.decision] || a.decision) },
      { t: 'Auditor', v: a => esc(a.auditor) },
      { t: 'Motivo y cambios', v: a => esc(a.comentario || '—') }
    ], d.auditorias)}` : ''}
    <h3 style="margin-top:12px">Historial</h3>
    ${tabla([
      { t: 'Fecha', v: h => fmt.fechaHora(h.fecha) },
      { t: 'Estado', v: h => badgeOrden({ estado: h.estadoNuevo }) },
      { t: 'Usuario', v: h => esc(h.usuario) },
      { t: 'Comentario', v: h => esc(h.comentario || '') }
    ], d.historial)}
    <div class="acciones">
      ${anulable ? '<button class="btn peligro" id="anular">Anular orden</button>' : ''}
      ${imprimible ? '<button class="btn" id="imprimir">Imprimir orden</button>' : ''}
      <button class="btn primario" data-cerrar>Cerrar</button>
    </div>`);
  $('#imprimir', m.el)?.addEventListener('click', e => conBoton(e.target, () => imprimir(id)));
  $('#anular', m.el)?.addEventListener('click', () => dialogoAnular(o, () => { m.cerrar(); alCambiar?.(); }));
}

export function tablaItems(d) {
  const cambio = d.items.some(i => i.cantidad !== i.cantidadSolicitada);
  return tabla([
    { t: 'Código', v: i => `<span class="mono">${esc(i.codigo)}</span>` },
    { t: 'Práctica', v: i => esc(i.descripcion) + (i.requiereAuditoria ? ' <span class="badge aviso">Requiere auditoría</span>' : '') },
    ...(cambio ? [{ t: 'Pedido', cls: 'num', v: i => i.cantidadSolicitada }] : []),
    { t: cambio ? 'Autorizado' : 'Cant.', cls: 'num', v: i => i.cantidad === 0 ? '<span class="badge error">No autorizada</span>' : i.cantidad },
    { t: 'A cargo del CMM', cls: 'num', v: i => fmt.moneda(i.cargoEntidad) },
    { t: 'Coseguro', cls: 'num', v: i => fmt.moneda(i.coseguro) }
  ], d.items, {
    pie: [{}, { v: 'Total' }, ...(cambio ? [{}] : []), {}, { cls: 'num', v: fmt.moneda(d.totalCargoEntidad) }, { cls: 'num', v: `<b>${fmt.moneda(d.orden.totalCoseguro)}</b>` }]
  });
}

function dialogoAnular(o, alTerminar) {
  const m = modal(`Anular la orden ${o.numero}`, `
    <div class="campo"><label>Motivo de la anulación *</label><input id="motivo" maxlength="250" placeholder="Ej.: El afiliado no la va a usar"></div>
    <p class="ayuda">Si el coseguro todavía no se cobró, se quita de la próxima cuota. Si ya se cobró, el sistema le avisa para que Liquidaciones haga la nota de crédito.</p>
    <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn peligro" id="ok">Anular</button></div>`);
  $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
    try {
      const r = await api.post(`/api/ordenes/${o.id}/anular`, { motivo: $('#motivo', m.el).value });
      m.cerrar();
      toast(`Orden ${o.numero} anulada`, 'ok');
      if (r.aviso) modal('Atención', `<div class="cinta aviso">${esc(r.aviso)}</div><div class="acciones"><button class="btn primario" data-cerrar>Entendido</button></div>`);
      alTerminar?.();
    } catch (er) { error(er); }
  }));
}
