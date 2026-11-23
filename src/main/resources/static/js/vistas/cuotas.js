import { api, fmt } from '../api.js';
import { html, raw, esc, $, $$, tabla, modal, datosForm, toast, error, opciones, cargando, conBoton, badgeEstado } from '../ui.js';
import { verComprobante } from './afiliados.js';
import { dialogoCoseguro, dialogoAnularCoseguro } from './coseguro-dialogo.js';

export async function render(el, params) {
  const tab = params[0] || 'generar';
  el.innerHTML = html`
    <div class="tarjeta" style="padding-bottom:0">
      <div class="tabs" id="tabs">
        ${raw([['generar', 'Generar cuotas del período'], ['coseguros', 'Coseguros'], ['valores', 'Valores de cuota'], ['resumen', 'Resumen de facturación']]
          .map(([k, t]) => `<button data-tab="${k}" class="${k === tab ? 'activo' : ''}">${t}</button>`).join(''))}
      </div>
    </div>
    <div id="contenido"></div>`;
  $$('#tabs button', el).forEach(b => b.addEventListener('click', () => { location.hash = `#/cuotas/${b.dataset.tab}`; }));
  const c = $('#contenido', el);
  cargando(c);
  try {
    if (tab === 'valores') await tabValores(c);
    else if (tab === 'resumen') await tabResumen(c, params[1]);
    else if (tab === 'coseguros') await tabCoseguros(c);
    else await tabGenerar(c);
  } catch (e) { error(e); }
}

// ---------------------------------------------------------------- Generación
function siguientePeriodo(periodos) {
  if (!periodos.length) { const h = fmt.hoy(); return h.substring(0, 7); }
  const ult = periodos[0];
  let a = Number(ult.substring(0, 4)), m = Number(ult.substring(4, 6)) + 1;
  if (m > 12) { m = 1; a++; }
  return `${a}-${String(m).padStart(2, '0')}`;
}

async function tabGenerar(c) {
  const periodos = await api.get('/api/facturacion/periodos');
  c.innerHTML = html`
    <div class="tarjeta">
      <h2>Generación masiva de cuotas</h2>
      <p class="ayuda">Por cada grupo familiar activo: cuota del plan × integrantes, más adicionales (Subsidio Alta Complejidad),
      los coseguros pendientes de cada integrante y el recargo por mora sobre la deuda vencida. Se emite un comprobante interno (no fiscal) al titular. Se recomienda simular antes de generar; los grupos que ya tienen la cuota del período se omiten.</p>
      <form id="form" class="fila">
        <div class="campo chico"><label>Período</label><input type="month" name="periodo" value="${siguientePeriodo(periodos)}" required></div>
        <div class="campo chico"><label>Fecha de emisión</label><input type="date" name="fechaEmision"></div>
        <button type="button" class="btn" id="simular">Simular</button>
        <button type="button" class="btn primario" id="generar">Generar</button>
      </form>
      <p class="suave" style="margin-top:8px">Períodos ya generados: ${periodos.length ? periodos.map(fmt.periodo).join(', ') : 'ninguno'}</p>
    </div>
    <div id="resultado"></div>`;
  const ejecutar = async (generar, btn) => conBoton(btn, async () => {
    try {
      const d = datosForm($('#form', c));
      if (!d.periodo) throw new Error('Indique el período.');
      if (generar && !confirm(`¿Generar las cuotas de ${d.periodo}? Esta acción impacta en las cuentas corrientes.`)) return;
      const r = await api.post(`/api/facturacion/cuotas/${generar ? 'generar' : 'simular'}`, d);
      mostrarResultado($('#resultado', c), r);
      if (generar) toast(`${r.generados} comprobantes generados`, 'ok');
    } catch (e) { error(e); }
  });
  $('#simular', c).addEventListener('click', e => ejecutar(false, e.target));
  $('#generar', c).addEventListener('click', e => ejecutar(true, e.target));
}

function mostrarResultado(cont, r) {
  cont.innerHTML = html`
    <div class="tarjeta">
      <div class="tarjeta-titulo"><h2>${r.simulacion ? 'Simulación' : 'Resultado'} · período ${fmt.periodo(r.periodo)}</h2>
        ${raw(r.simulacion ? '<span class="badge info">No se guardó nada</span>' : '<span class="badge ok">Generado</span>')}</div>
      <div class="grid-kpi">
        <div class="kpi"><div class="kpi-etiqueta">Comprobantes</div><div class="kpi-valor">${r.generados}</div><div class="kpi-nota">${r.omitidosYaExistian} ya existían</div></div>
        <div class="kpi"><div class="kpi-etiqueta">Cuotas + adicionales</div><div class="kpi-valor">${fmt.moneda(r.totalCuotas)}</div></div>
        <div class="kpi"><div class="kpi-etiqueta">Coseguros</div><div class="kpi-valor">${fmt.moneda(r.totalCoseguros)}</div></div>
        <div class="kpi"><div class="kpi-etiqueta">Recargos por mora</div><div class="kpi-valor">${fmt.moneda(r.totalRecargos)}</div><div class="kpi-nota">${r.conRecargo} grupo(s)</div></div>
        <div class="kpi"><div class="kpi-etiqueta">Total</div><div class="kpi-valor">${fmt.moneda(r.totalGeneral)}</div><div class="kpi-nota">vence ${fmt.fecha(r.fechaVencimiento)}</div></div>
      </div>
      ${raw(r.advertencias.length ? `<div class="cinta aviso">${r.advertencias.map(esc).join('<br>')}</div>` : '')}
      ${raw(tabla([
        { t: 'Nro. afiliado', v: p => `<span class="mono">${esc(p.nroAfiliado)}</span>` },
        { t: 'Titular', v: p => `<a href="#/afiliados/${p.afiliadoId}/cuenta">${esc(p.apellidoNombre)}</a> ${p.observacion ? `<span class="badge error">${esc(p.observacion)}</span>` : ''}` },
        { t: 'Plan', v: p => esc(p.planCodigo) },
        { t: 'Integrantes', cls: 'num', v: p => p.integrantes },
        { t: 'Cuota', cls: 'num', v: p => fmt.moneda(p.cuota) },
        { t: 'Adicionales', cls: 'num', v: p => fmt.moneda(p.adicionales) },
        { t: 'Coseguros', cls: 'num', v: p => p.coseguros > 0 ? `${fmt.moneda(p.coseguros)} <span class="suave">(${p.cantidadCoseguros})</span>` : '' },
        { t: 'Deuda vencida', cls: 'num', v: p => p.deudaVencida > 0 ? fmt.moneda(p.deudaVencida) : '' },
        { t: 'Recargo', cls: 'num', v: p => p.recargo > 0 ? fmt.moneda(p.recargo) : '' },
        { t: 'Total', cls: 'num', v: p => `<b>${fmt.moneda(p.total)}</b>` }
      ], r.comprobantes, { vacio: 'No hay comprobantes para generar.' }))}
    </div>`;
}

// ---------------------------------------------------------------- Coseguros (T42, T43)
const aMes = p => `${p.substring(0, 4)}-${p.substring(4, 6)}`;
const sumarMeses = (mes, n) => {
  const [a, m] = mes.split('-').map(Number);
  const d = new Date(a, m - 1 + n, 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
};

async function tabCoseguros(c) {
  const actual = fmt.hoy().substring(0, 7);
  const filtro = { desde: sumarMeses(actual, -5), hasta: sumarMeses(actual, 1), estado: '' };
  c.innerHTML = html`
    <div class="tarjeta">
      <div class="tarjeta-titulo">
        <h2>Coseguros a cobrar en la cuota</h2>
        <button class="btn primario" id="nuevo">Cargar coseguro</button>
      </div>
      <p class="ayuda">Cada orden emitida deja su coseguro pendiente a nombre del integrante; al generar la cuota del mes siguiente se suma
      solo al comprobante del grupo. Si la orden no se usó ni se anuló, el coseguro se cobra igual. Mientras el módulo de órdenes no esté
      activo, los coseguros se cargan desde aquí.</p>
      <form id="filtro" class="fila">
        <div class="campo chico"><label>Cuota desde</label><input type="month" name="desde" value="${filtro.desde}"></div>
        <div class="campo chico"><label>Cuota hasta</label><input type="month" name="hasta" value="${filtro.hasta}"></div>
        <div class="campo chico"><label>Estado</label><select name="estado">
          <option value="">Todos</option><option value="PENDIENTE">Pendientes</option><option value="FACTURADO">Facturados</option><option value="ANULADO">Anulados</option>
        </select></div>
        <button class="btn">Ver</button>
      </form>
    </div>
    <div id="resCos"></div>`;
  const cargar = async () => {
    const cont = $('#resCos', c);
    cargando(cont);
    try {
      const f = datosForm($('#filtro', c));
      const r = await api.get('/api/facturacion/coseguros', { desde: f.desde, hasta: f.hasta, estado: f.estado });
      cont.innerHTML = html`
        <div class="tarjeta">
          <div class="grid-kpi">
            <div class="kpi"><div class="kpi-etiqueta">Pendientes de facturar</div><div class="kpi-valor">${fmt.moneda(r.totalPendiente)}</div></div>
            <div class="kpi"><div class="kpi-etiqueta">Facturados en cuotas</div><div class="kpi-valor">${fmt.moneda(r.totalFacturado)}</div></div>
            <div class="kpi"><div class="kpi-etiqueta">Coseguros</div><div class="kpi-valor">${r.cargos.filter(x => x.estado !== 'ANULADO').length}</div></div>
          </div>
          <h3>Por mes de cuota</h3>
          ${raw(tabla([
            { t: 'Cuota', v: x => fmt.periodo(x.periodo) },
            { t: 'Coseguros', cls: 'num', v: x => x.cantidad },
            { t: 'Pendiente', cls: 'num', v: x => fmt.moneda(x.pendiente) },
            { t: 'Facturado', cls: 'num', v: x => fmt.moneda(x.facturado) },
            { t: 'Anulado', cls: 'num', v: x => x.anulado > 0 ? fmt.moneda(x.anulado) : '' }
          ], r.porPeriodo, { vacio: 'Sin coseguros en el rango.' }))}
          <h3>Detalle</h3>
          ${raw(tablaCoseguros(r.cargos, true))}
        </div>`;
      enlazarAcciones(cont, r.cargos, cargar);
    } catch (e) { error(e); }
  };
  $('#filtro', c).addEventListener('submit', e => { e.preventDefault(); cargar(); });
  $('#nuevo', c).addEventListener('click', () => dialogoCoseguro({ alTerminar: cargar }));
  await cargar();
}

/** Tabla de coseguros (también la usa la ficha del afiliado). */
export function tablaCoseguros(cargos, conTitular) {
  return tabla([
    { t: 'Cuota', v: x => fmt.periodo(x.periodoDestino) },
    ...(conTitular ? [{ t: 'Grupo', v: x => `<a href="#/afiliados/${x.titularId}/cuenta">${esc(x.titularNombre)}</a>` }] : []),
    { t: 'Integrante', v: x => `<span class="mono suave">${esc(x.afiliadoNro)}</span> ${esc(x.afiliadoNombre)}` },
    { t: 'Fecha', v: x => fmt.fecha(x.fecha) },
    { t: 'Concepto', v: x => esc(x.concepto) + (x.origenRef ? ` <span class="suave">· orden ${esc(x.origenRef)}</span>` : '') },
    { t: 'Importe', cls: 'num', v: x => fmt.moneda(x.importe) },
    { t: 'Estado', v: x => badgeEstado(x.estado) + (x.comprobanteNumero ? ` <a href="#" data-comp="${x.comprobanteId}" class="mono">${esc(x.comprobanteNumero)}</a>` : '')
        + (x.motivoAnulacion ? `<div class="suave">${esc(x.motivoAnulacion)}</div>` : '') },
    { t: 'Cargó', v: x => `<span class="suave">${esc(x.origen === 'ORDEN' ? 'Orden' : (x.usuario || ''))}</span>` },
    { t: '', cls: 'derecha', v: x => x.estado === 'PENDIENTE' ? `<button class="btn chico" data-anular="${x.id}">Anular</button>` : '' }
  ], cargos, { vacio: 'No hay coseguros.' });
}

export function enlazarAcciones(cont, cargos, recargar) {
  $$('[data-comp]', cont).forEach(l => l.addEventListener('click', e => { e.preventDefault(); verComprobante(l.dataset.comp); }));
  $$('[data-anular]', cont).forEach(b => b.addEventListener('click', () => {
    dialogoAnularCoseguro(cargos.find(x => String(x.id) === b.dataset.anular), recargar);
  }));
}

// ---------------------------------------------------------------- Valores de cuota
async function tabValores(c) {
  const [valores, ipc] = await Promise.all([api.get('/api/facturacion/valores-cuota'), api.get('/api/facturacion/ipc')]);
  c.innerHTML = html`
    <div class="tarjeta">
      <div class="tarjeta-titulo">
        <h2>IPC mensual (INDEC)</h2>
        <div class="fila"><button class="btn" id="ipcManual">Cargar a mano</button><button class="btn primario" id="ipcTraer">Traer de la API del INDEC</button></div>
      </div>
      <p class="ayuda">La cuota se actualiza por la variación mensual del IPC. El sistema la consulta en la API de series de tiempo del Estado nacional
      (o se carga a mano) y Liquidaciones confirma la vigencia y los planes antes de aplicarla. Cada mes se aplica una sola vez.</p>
      ${raw(tabla([
        { t: 'Mes medido', v: i => fmt.periodo(i.periodo) },
        { t: 'Variación', cls: 'num', v: i => `<b>${fmt.pct(i.variacion)}</b>` },
        { t: 'Origen', v: i => i.origen === 'API' ? 'API del INDEC' : `Carga manual <span class="suave">(${esc(i.usuario || '')})</span>` },
        { t: 'Aplicado', v: i => i.aplicadoVigencia ? `<span class="badge ok">Vigente desde ${fmt.fecha(i.aplicadoVigencia)}</span>` : '<span class="badge aviso">Sin aplicar</span>' },
        { t: '', cls: 'derecha', v: i => i.aplicadoVigencia ? '' : `<button class="btn chico primario" data-ipc="${i.periodo}">Aplicar a las cuotas</button>` }
      ], ipc.slice(0, 12), { vacio: 'Todavía no hay valores de IPC. Tráigalos de la API o cárguelos a mano.' }))}
    </div>
    <div class="tarjeta">
      <div class="tarjeta-titulo">
        <h2>Valor de la cuota por integrante</h2>
        <div class="fila"><button class="btn" id="puntual">Cargar valor puntual</button><button class="btn primario" id="actualizar">Actualizar por porcentaje</button></div>
      </div>
      <p class="ayuda">Cada actualización crea una nueva vigencia a partir de la fecha indicada y queda registrado quién la realizó.</p>
      ${raw(tabla([
        { t: 'Plan', v: v => `<b class="mono">${esc(v.planCodigo)}</b> ${esc(v.planNombre)}` },
        { t: 'Cobertura', cls: 'num', v: v => fmt.pct(v.coberturaPorcentaje) },
        { t: 'Vigente desde', v: v => fmt.fecha(v.vigenteDesde) },
        { t: 'Valor vigente', cls: 'num', v: v => `<b>${fmt.moneda(v.importeVigente)}</b>` },
        { t: 'Historial', v: v => v.historial.map(h => `<div class="suave">${fmt.fecha(h.vigenteDesde)} · ${fmt.moneda(h.importe)} · ${esc(h.motivo || '')} (${esc(h.usuario || '')})</div>`).join('') }
      ], valores))}
    </div>`;
  $('#ipcTraer', c).addEventListener('click', e => conBoton(e.target, async () => {
    try {
      const r = await api.post('/api/facturacion/ipc/traer');
      toast(`IPC actualizado: ${r.agregados} mes(es) nuevos; último publicado ${fmt.periodo(r.ultimoPeriodo)}`, 'ok');
      tabValores(c);
    } catch (er) { error(er); }
  }));
  $('#ipcManual', c).addEventListener('click', () => {
    const m = modal('Cargar IPC a mano', `
      <form class="form-grid">
        <div class="campo"><label>Mes medido *</label><input type="month" name="periodo" required></div>
        <div class="campo"><label>Variación mensual (%) *</label><input name="variacion" data-tipo="num" placeholder="Ej.: 2,3" required></div>
      </form>
      <p class="ayuda">Dato publicado por el INDEC (variación del nivel general respecto del mes anterior).</p>
      <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn primario" id="ok">Guardar</button></div>`);
    $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
      try { await api.post('/api/facturacion/ipc', datosForm($('form', m.el))); m.cerrar(); toast('IPC guardado', 'ok'); tabValores(c); } catch (er) { error(er); }
    }));
  });
  $$('[data-ipc]', c).forEach(b => b.addEventListener('click', () => {
    const i = ipc.find(x => x.periodo === b.dataset.ipc);
    const h = fmt.hoy();
    let [a, mm] = h.substring(0, 7).split('-').map(Number); mm++; if (mm > 12) { mm = 1; a++; }
    const vig = `${a}-${String(mm).padStart(2, '0')}-01`;
    const m = modal(`Aplicar IPC ${fmt.periodo(i.periodo)}: ${fmt.pct(i.variacion)}`, `
      <form class="form-grid">
        <div class="campo"><label>Nuevos valores vigentes desde *</label><input type="date" name="vigenteDesde" value="${vig}" required></div>
      </form>
      <h3>Planes alcanzados</h3>
      <div>${valores.map(v => `<label style="display:inline-flex;gap:6px;margin:0 14px 6px 0"><input type="checkbox" class="pl" value="${v.planId}" checked> ${esc(v.planCodigo)}</label>`).join('')}</div>
      <div class="cinta aviso" style="margin-top:10px">Se crea una nueva vigencia por plan con el valor actual + ${fmt.pct(i.variacion)}. Revise la fecha: las cuotas de ese mes en adelante usan los valores nuevos.</div>
      <div id="res"></div>
      <div class="acciones"><button class="btn" data-cerrar>Cerrar</button><button class="btn primario" id="ok">Confirmar y aplicar</button></div>`);
    $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
      try {
        const d = datosForm($('form', m.el));
        d.planIds = $$('.pl', m.el).filter(x => x.checked).map(x => Number(x.value));
        const r = await api.post(`/api/facturacion/ipc/${i.periodo}/aplicar`, d);
        $('#res', m.el).innerHTML = `<div class="cinta ok">${r.planesActualizados} plan(es) actualizados<br>${r.detalle.map(esc).join('<br>')}</div>`;
        e.target.remove();
        tabValores(c);
      } catch (er) { error(er); }
    }));
  }));
  $('#actualizar', c).addEventListener('click', () => {
    const m = modal('Actualizar cuotas por porcentaje', `
      <form class="form-grid">
        <div class="campo"><label>Porcentaje *</label><input name="porcentaje" data-tipo="num" placeholder="Ej.: 3,5" required></div>
        <div class="campo"><label>Vigente desde *</label><input type="date" name="vigenteDesde" required></div>
        <div class="campo" style="grid-column:1/-1"><label>Motivo</label><input name="motivo" placeholder="Ej.: Aumento autorizado por Comisión Directiva"></div>
      </form>
      <h3>Planes alcanzados</h3>
      <div>${valores.map(v => `<label style="display:inline-flex;gap:6px;margin:0 14px 6px 0"><input type="checkbox" class="pl" value="${v.planId}" checked> ${esc(v.planCodigo)}</label>`).join('')}</div>
      <div id="res"></div>
      <div class="acciones"><button class="btn" data-cerrar>Cerrar</button><button class="btn primario" id="ok">Aplicar</button></div>`);
    $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
      try {
        const d = datosForm($('form', m.el));
        d.planIds = $$('.pl', m.el).filter(x => x.checked).map(x => Number(x.value));
        const r = await api.post('/api/facturacion/valores-cuota/actualizar', d);
        $('#res', m.el).innerHTML = `<div class="cinta ok">${r.planesActualizados} plan(es) actualizados<br>${r.detalle.map(esc).join('<br>')}</div>`;
        tabValores(c);
      } catch (er) { error(er); }
    }));
  });
  $('#puntual', c).addEventListener('click', () => {
    const m = modal('Cargar valor puntual', `
      <form class="form-grid">
        <div class="campo"><label>Plan *</label><select name="planId">${opciones(valores, null, { valor: 'planId', texto: v => `${v.planCodigo} ${v.planNombre}` })}</select></div>
        <div class="campo"><label>Vigente desde *</label><input type="date" name="vigenteDesde" required></div>
        <div class="campo"><label>Valor por integrante *</label><input name="importe" data-tipo="num" required></div>
        <div class="campo"><label>Motivo</label><input name="motivo"></div>
      </form>
      <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn primario" id="ok">Guardar</button></div>`);
    $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
      try {
        const d = datosForm($('form', m.el)); d.planId = Number(d.planId);
        await api.post('/api/facturacion/valores-cuota', d);
        m.cerrar(); toast('Valor guardado', 'ok'); tabValores(c);
      } catch (er) { error(er); }
    }));
  });
}

// ---------------------------------------------------------------- Resumen
async function tabResumen(c, periodoSel) {
  const periodos = await api.get('/api/facturacion/periodos');
  if (!periodos.length) { c.innerHTML = '<div class="tarjeta"><div class="vacio">Todavía no se generaron cuotas.</div></div>'; return; }
  const periodo = periodoSel || periodos[0];
  const r = await api.get('/api/facturacion/resumen', { periodo });
  c.innerHTML = html`
    <div class="tarjeta">
      <div class="tarjeta-titulo"><h2>Resumen de facturación</h2>
        <div class="campo chico"><select id="selPeriodo">${raw(opciones(periodos.map(p => ({ id: p, nombre: fmt.periodo(p) })), periodo))}</select></div></div>
      <div class="grid-kpi">
        <div class="kpi"><div class="kpi-etiqueta">Comprobantes</div><div class="kpi-valor">${r.comprobantes}</div></div>
        <div class="kpi"><div class="kpi-etiqueta">Facturado</div><div class="kpi-valor">${fmt.moneda(r.total)}</div></div>
        <div class="kpi"><div class="kpi-etiqueta">Cobrado</div><div class="kpi-valor">${fmt.moneda(r.cobrado)}</div><div class="kpi-nota">${fmt.pct(r.porcentajeCobrado)} del total</div></div>
        <div class="kpi ${raw(r.pendiente > 0 ? 'alerta' : '')}"><div class="kpi-etiqueta">Pendiente</div><div class="kpi-valor">${fmt.moneda(r.pendiente)}</div></div>
      </div>
      <h3>Por concepto (código de servicio → cuenta contable)</h3>
      ${raw(tabla([
        { t: 'Código', v: x => `<span class="mono">${esc(x.codigo)}</span>` },
        { t: 'Concepto', v: x => esc(x.descripcion) },
        { t: 'Cuenta contable', v: x => `<span class="mono">${esc(x.cuentaContable)}</span>` },
        { t: 'Cantidad', cls: 'num', v: x => fmt.numero(x.cantidad) },
        { t: 'Importe', cls: 'num', v: x => fmt.moneda(x.importe) }
      ], r.porConcepto))}
      <h3>Por plan</h3>
      ${raw(tabla([
        { t: 'Plan', v: x => `<b class="mono">${esc(x.planCodigo)}</b> ${esc(x.planNombre)}` },
        { t: 'Grupos', cls: 'num', v: x => x.comprobantes },
        { t: 'Facturado', cls: 'num', v: x => fmt.moneda(x.total) },
        { t: 'Pendiente', cls: 'num', v: x => fmt.moneda(x.pendiente) }
      ], r.porPlan))}
    </div>`;
  $('#selPeriodo', c).addEventListener('change', e => { location.hash = `#/cuotas/resumen/${e.target.value}`; });
}

export { verComprobante };
