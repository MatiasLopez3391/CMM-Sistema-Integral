import { api, fmt } from '../api.js';
import { html, raw, esc, $, $$, tabla, modal, datosForm, toast, error, opciones, cargando, conBoton } from '../ui.js';
import { verComprobante } from './afiliados.js';

export async function render(el, params) {
  const tab = params[0] || 'generar';
  el.innerHTML = html`
    <div class="tarjeta" style="padding-bottom:0">
      <div class="tabs" id="tabs">
        ${raw([['generar', 'Generar cuotas del período'], ['valores', 'Valores de cuota'], ['resumen', 'Resumen de facturación']]
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
      <p class="ayuda">Por cada grupo familiar activo: cuota del plan × integrantes, más adicionales (Subsidio Alta Complejidad) y recargo por mora
      sobre la deuda vencida. Se emite un comprobante interno (no fiscal) al titular. Se recomienda simular antes de generar; los grupos que ya tienen la cuota del período se omiten.</p>
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
        { t: 'Deuda vencida', cls: 'num', v: p => p.deudaVencida > 0 ? fmt.moneda(p.deudaVencida) : '' },
        { t: 'Recargo', cls: 'num', v: p => p.recargo > 0 ? fmt.moneda(p.recargo) : '' },
        { t: 'Total', cls: 'num', v: p => `<b>${fmt.moneda(p.total)}</b>` }
      ], r.comprobantes, { vacio: 'No hay comprobantes para generar.' }))}
    </div>`;
}

// ---------------------------------------------------------------- Valores de cuota
async function tabValores(c) {
  const valores = await api.get('/api/facturacion/valores-cuota');
  c.innerHTML = html`
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
