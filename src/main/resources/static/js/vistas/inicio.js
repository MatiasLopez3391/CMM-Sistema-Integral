import { api, fmt } from '../api.js';
import { html, raw, cargando } from '../ui.js';

export async function render(el) {
  cargando(el);
  const t = await api.get('/api/tablero');
  const pctCobrado = t.ultimoPeriodoTotal > 0 ? (t.ultimoPeriodoCobrado * 100 / t.ultimoPeriodoTotal) : 0;
  const max = Math.max(1, ...t.evolucion.map(e => Number(e.facturado)));
  const barras = t.evolucion.map(e => html`
    <div class="barra-grupo" title="Facturado ${fmt.moneda(e.facturado)} · Cobrado ${fmt.moneda(e.cobrado)}">
      <div class="barra-par">
        <div class="barra-v fact" style="height:${raw((Number(e.facturado) / max * 100).toFixed(1))}%"></div>
        <div class="barra-v cob" style="height:${raw((Number(e.cobrado) / max * 100).toFixed(1))}%"></div>
      </div>
      <div class="barra-etq">${fmt.periodo(e.periodo)}</div>
    </div>`);

  el.innerHTML = html`
    <div class="grid-kpi">
      <div class="kpi"><div class="kpi-etiqueta">Afiliados activos</div><div class="kpi-valor">${fmt.numero(t.afiliadosActivos)}</div>
        <div class="kpi-nota">${fmt.numero(t.gruposActivos)} grupos familiares</div></div>
      <div class="kpi ${raw(t.deudaVencida > 0 ? 'alerta' : '')}"><div class="kpi-etiqueta">Deuda vencida</div>
        <div class="kpi-valor">${fmt.moneda(t.deudaVencida)}</div><div class="kpi-nota">${fmt.numero(t.gruposMorosos)} grupos con cuotas vencidas</div></div>
      <div class="kpi"><div class="kpi-etiqueta">Grupos bloqueados</div><div class="kpi-valor">${fmt.numero(t.gruposBloqueados)}</div>
        <div class="kpi-nota">Activos pero sin órdenes</div></div>
      <div class="kpi"><div class="kpi-etiqueta">Cuota ${fmt.periodo(t.ultimoPeriodo)}</div>
        <div class="kpi-valor">${fmt.pct(pctCobrado.toFixed(1))}</div>
        <div class="kpi-nota">cobrado de ${fmt.moneda(t.ultimoPeriodoTotal)}</div></div>
      <div class="kpi"><div class="kpi-etiqueta">Cobrado hoy</div><div class="kpi-valor">${fmt.moneda(t.cobradoHoy)}</div>
        <div class="kpi-nota">${raw(t.lotesPendientes > 0 ? `<a href="#/cobranza">${t.lotesPendientes} archivo(s) sin confirmar</a>` : 'Sin archivos pendientes')}</div></div>
    </div>
    <div class="tarjeta">
      <div class="tarjeta-titulo"><h2>Facturado y cobrado por período</h2><a href="#/cuotas/resumen">Ver resumen de facturación →</a></div>
      ${t.evolucion.length ? raw(`<div class="barras">${barras.join('')}</div>
        <div class="leyenda"><span><i style="background:#a9c6e3"></i>Facturado</span><span><i style="background:var(--azul)"></i>Cobrado</span></div>`)
        : raw('<div class="vacio">Todavía no se generaron cuotas.</div>')}
    </div>
    <div class="tarjeta">
      <h2>Accesos rápidos</h2>
      <div class="fila">
        <a class="btn" href="#/afiliados">Buscar afiliado</a>
        <a class="btn" href="#/cuotas/generar">Generar cuotas</a>
        <a class="btn" href="#/cobranza/ventanilla">Pago en ventanilla</a>
        <a class="btn" href="#/cobranza">Importar archivo de cobro</a>
        <a class="btn" href="#/morosos">Ver morosos</a>
      </div>
    </div>`;
}
