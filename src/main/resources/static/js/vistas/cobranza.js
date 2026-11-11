import { api, fmt } from '../api.js';
import { html, raw, esc, $, $$, tabla, modal, toast, error, opciones, badgeEstado, badgeBloqueado, cargando, conBoton } from '../ui.js';
import { dialogoPagoVentanilla } from './afiliados.js';

export async function render(el, params) {
  const tab = params[0] || 'importar';
  el.innerHTML = html`
    <div class="tarjeta" style="padding-bottom:0">
      <div class="tabs" id="tabs">
        ${raw([['importar', 'Importar archivo de cobro'], ['ventanilla', 'Pago en ventanilla'], ['lotes', 'Archivos importados'], ['caja', 'Caja diaria']]
          .map(([k, t]) => `<button data-tab="${k}" class="${k === tab ? 'activo' : ''}">${t}</button>`).join(''))}
      </div>
    </div>
    <div id="contenido"></div>`;
  $$('#tabs button', el).forEach(b => b.addEventListener('click', () => { location.hash = `#/cobranza/${b.dataset.tab}`; }));
  const c = $('#contenido', el);
  cargando(c);
  try {
    if (tab === 'ventanilla') await tabVentanilla(c);
    else if (tab === 'lotes') await tabLotes(c);
    else if (tab === 'caja') await tabCaja(c);
    else await tabImportar(c);
  } catch (e) { error(e); }
}

// ---------------------------------------------------------------- Importación
async function tabImportar(c) {
  const medios = (await api.get('/api/cobranza/medios')).filter(m => m.admiteArchivo);
  const preseleccion = (medios.find(m => m.codigo === 'PMC') || medios.find(m => m.layoutId) || medios[0])?.id;
  c.innerHTML = html`
    <div class="tarjeta">
      <h2>Importar archivo de un ente de cobro</h2>
      <p class="ayuda">Paso 1: previsualizar (valida cada línea sin tocar las cuentas). Paso 2: confirmar (imputa los pagos válidos a las cuotas más antiguas).
      Se rechazan afiliados inexistentes, importes inválidos, referencias repetidas y archivos ya importados.</p>
      <form id="form" class="fila">
        <div class="campo"><label>Ente de cobro</label><select name="medioId" id="selMedio">${raw(opciones(medios, preseleccion, { texto: m => m.nombre + (m.layoutId ? '' : ' (sin diseño de archivo)') }))}</select></div>
        <div class="campo"><label>Archivo (.txt / .csv)</label><input type="file" name="archivo" accept=".txt,.csv,.dat"></div>
        <button type="button" class="btn primario" id="previsualizar">Previsualizar</button>
      </form>
      <div id="avisoLayout" style="margin-top:10px"></div>
    </div>
    <div id="preview"></div>`;
  const avisoLayout = () => {
    const m = medios.find(x => String(x.id) === $('#selMedio', c).value);
    $('#avisoLayout', c).innerHTML = !m ? '' : !m.layoutId
      ? '<div class="cinta error">Este ente no tiene diseño de archivo configurado.</div>'
      : `<p class="suave">Diseño de archivo: ${esc(m.layoutNombre)}</p>`;
  };
  $('#selMedio', c).addEventListener('change', avisoLayout);
  avisoLayout();
  $('#previsualizar', c).addEventListener('click', e => conBoton(e.target, async () => {
    try {
      const f = $('#form', c);
      if (!f.archivo.files[0]) throw new Error('Seleccione el archivo.');
      const fd = new FormData(); fd.append('archivo', f.archivo.files[0]);
      const lote = await api.multipart('/api/cobranza/lotes/previsualizar', fd, { medioId: f.medioId.value });
      mostrarLote($('#preview', c), lote, () => { f.reset(); avisoLayout(); });
    } catch (er) { error(er); }
  }));
}

function tablaPagos(pagos) {
  return tabla([
    { t: 'Línea', cls: 'num', v: p => p.linea ?? '' },
    { t: 'Nro. afiliado', v: p => `<span class="mono">${esc(p.nroAfiliado || '')}</span>` },
    { t: 'Afiliado', v: p => p.afiliadoId ? `<a href="#/afiliados/${p.afiliadoId}/cuenta">${esc(p.afiliado)}</a>` : '<span class="suave">—</span>' },
    { t: 'Fecha', v: p => fmt.fecha(p.fechaPago) },
    { t: 'Referencia', v: p => `<span class="mono">${esc(p.referencia || '')}</span>` },
    { t: 'Importe', cls: 'num', v: p => fmt.moneda(p.importe) },
    { t: 'Estado', v: p => badgeEstado(p.estado) + (p.motivoRechazo ? `<div class="suave" style="font-size:12px">${esc(p.motivoRechazo)}</div>` : '') }
  ], pagos);
}

function mostrarLote(cont, lote, alTerminar) {
  const pendiente = lote.estado === 'PREVISUALIZADO';
  cont.innerHTML = html`
    <div class="tarjeta">
      <div class="tarjeta-titulo"><h2>${lote.nombreArchivo} · ${lote.medio}</h2>${raw(badgeEstado(lote.estado))}</div>
      <div class="grid-kpi">
        <div class="kpi"><div class="kpi-etiqueta">Registros</div><div class="kpi-valor">${lote.cantidadRegistros}</div></div>
        <div class="kpi"><div class="kpi-etiqueta">Válidos</div><div class="kpi-valor">${lote.cantidadOk}</div><div class="kpi-nota">${fmt.moneda(lote.importeOk)}</div></div>
        <div class="kpi ${raw(lote.cantidadRechazados ? 'alerta' : '')}"><div class="kpi-etiqueta">Rechazados</div><div class="kpi-valor">${lote.cantidadRechazados}</div></div>
      </div>
      ${raw(tablaPagos(lote.pagos))}
      <div id="resConf"></div>
      ${raw(pendiente ? `<div class="acciones"><button class="btn peligro" id="descartar">Descartar</button>
        <button class="btn primario" id="confirmar" ${lote.cantidadOk ? '' : 'disabled'}>Confirmar e imputar ${lote.cantidadOk} pago(s)</button></div>` : '')}
    </div>`;
  $('#confirmar', cont)?.addEventListener('click', e => conBoton(e.target, async () => {
    try {
      const r = await api.post(`/api/cobranza/lotes/${lote.id}/confirmar`);
      toast(`${r.pagosImputados} pagos imputados`, 'ok');
      const actualizado = await api.get(`/api/cobranza/lotes/${lote.id}`);
      mostrarLote(cont, actualizado);
      $('#resConf', cont).innerHTML = html`<div class="cinta ok">Imputado ${fmt.moneda(r.importeImputado)}
        ${raw(r.importeACuenta > 0 ? ` · ${fmt.moneda(r.importeACuenta)} quedó a cuenta (saldo a favor)` : '')}
        ${raw(r.comprobantesCancelados.length ? `<br>Comprobantes cancelados: ${r.comprobantesCancelados.map(esc).join(', ')}` : '')}</div>`;
      alTerminar && alTerminar();
    } catch (er) { error(er); }
  }));
  $('#descartar', cont)?.addEventListener('click', e => conBoton(e.target, async () => {
    try { await api.post(`/api/cobranza/lotes/${lote.id}/descartar`); toast('Lote descartado'); cont.innerHTML = ''; } catch (er) { error(er); }
  }));
}

// ---------------------------------------------------------------- Ventanilla
async function tabVentanilla(c) {
  c.innerHTML = html`
    <div class="tarjeta">
      <h2>Pago en ventanilla</h2>
      <p class="ayuda">Buscá al afiliado, cargá el pago y adjuntá el comprobante (JPG, PNG o PDF). El pago se imputa en el momento
      a las cuotas más antiguas del grupo familiar.</p>
      <form id="buscar" class="fila">
        <div class="campo"><label>Nro. afiliado, documento o apellido</label><input name="q" required></div>
        <button class="btn primario">Buscar</button>
      </form>
      <div id="res" style="margin-top:12px"></div>
    </div>
    <div class="tarjeta"><h2>Últimos pagos en ventanilla</h2><div id="ultimos"></div></div>`;
  const ultimos = async () => {
    const pagos = await api.get('/api/cobranza/pagos');
    $('#ultimos', c).innerHTML = tabla([
      { t: 'Registrado', v: p => fmt.fechaHora(p.registradoEl) + `<div class="suave">${esc(p.registradoPor || '')}</div>` },
      { t: 'Afiliado', v: p => `<a href="#/afiliados/${p.afiliadoId}/cuenta">${esc(p.afiliado)}</a>` },
      { t: 'Fecha de pago', v: p => fmt.fecha(p.fechaPago) },
      { t: 'Importe', cls: 'num', v: p => fmt.moneda(p.importe) },
      { t: 'Comprobante', v: p => p.tieneAdjunto ? `<a href="/api/cobranza/pagos/${p.id}/comprobante" target="_blank">${esc(p.adjuntoNombre)}</a>` : '' },
      { t: 'Observaciones', v: p => esc(p.observaciones || '') }
    ], pagos, { vacio: 'Todavía no hay pagos en ventanilla.' });
  };
  $('#buscar', c).addEventListener('submit', async e => {
    e.preventDefault();
    try {
      const r = await api.get('/api/afiliados', { q: e.target.q.value, estado: 'ACTIVO', soloTitulares: false, tamanio: 10 });
      $('#res', c).innerHTML = tabla([
        { t: 'Nro. afiliado', v: a => `<span class="mono">${esc(a.nroAfiliado)}</span>` },
        { t: 'Apellido y nombre', v: a => esc(a.apellidoNombre) + ' ' + badgeBloqueado(a.bloqueado) },
        { t: 'Plan', v: a => esc(a.planCodigo) },
        { t: '', cls: 'derecha', v: () => '<button class="btn chico primario">Registrar pago</button>' }
      ], r.contenido, { vacio: 'Sin resultados.' });
      $$('#res tbody tr', c).forEach(tr => {
        const a = r.contenido[tr.dataset.i];
        tr.querySelector('button').addEventListener('click', () => dialogoPagoVentanilla(a.id, a.apellidoNombre, ultimos));
      });
    } catch (er) { error(er); }
  });
  await ultimos();
}

// ---------------------------------------------------------------- Lotes
async function tabLotes(c) {
  const lotes = await api.get('/api/cobranza/lotes');
  c.innerHTML = html`<div class="tarjeta"><h2>Archivos importados</h2>${raw(tabla([
    { t: 'Lote', v: l => '#' + l.id },
    { t: 'Ente', v: l => esc(l.medio) },
    { t: 'Archivo', v: l => `<span class="mono">${esc(l.nombreArchivo)}</span>` },
    { t: 'Importado', v: l => fmt.fechaHora(l.creadoEl) + `<div class="suave">${esc(l.creadoPor || '')}</div>` },
    { t: 'Registros', cls: 'num', v: l => l.cantidadRegistros },
    { t: 'Válidos', cls: 'num', v: l => l.cantidadOk },
    { t: 'Rechazados', cls: 'num', v: l => l.cantidadRechazados },
    { t: 'Importe', cls: 'num', v: l => fmt.moneda(l.importeOk) },
    { t: 'Estado', v: l => badgeEstado(l.estado) }
  ], lotes, { clic: true, vacio: 'No se importaron archivos.' }))}</div><div id="detalle"></div>`;
  $$('tbody tr', c).forEach(tr => tr.addEventListener('click', async () => {
    try {
      const l = await api.get(`/api/cobranza/lotes/${lotes[tr.dataset.i].id}`);
      mostrarLote($('#detalle', c), l, () => tabLotes(c));
      $('#detalle', c).scrollIntoView({ behavior: 'smooth' });
    } catch (e) { error(e); }
  }));
}

// ---------------------------------------------------------------- Caja
async function tabCaja(c, fecha) {
  const f = fecha || fmt.hoy();
  const r = await api.get('/api/cobranza/caja', { fecha: f });
  c.innerHTML = html`
    <div class="tarjeta">
      <div class="tarjeta-titulo"><h2>Cobranza del día</h2><div class="campo chico"><input type="date" id="fecha" value="${f}"></div></div>
      <div class="grid-kpi">
        <div class="kpi"><div class="kpi-etiqueta">Total cobrado</div><div class="kpi-valor">${fmt.moneda(r.total)}</div><div class="kpi-nota">${r.cantidad} pago(s)</div></div>
        ${raw(r.porMedio.map(m => `<div class="kpi"><div class="kpi-etiqueta">${esc(m.medio)}</div><div class="kpi-valor">${fmt.moneda(m.total)}</div><div class="kpi-nota">${m.cantidad} pago(s)</div></div>`).join(''))}
      </div>
      ${raw(tablaPagos(r.pagos))}
    </div>`;
  $('#fecha', c).addEventListener('change', e => tabCaja(c, e.target.value).catch(error));
}
