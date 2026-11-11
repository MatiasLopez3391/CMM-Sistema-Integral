import { api, fmt } from '../api.js';
import { html, raw, esc, $, tabla, toast, error, cargando, conBoton } from '../ui.js';

export async function render(el) {
  cargando(el);
  const codigos = await api.get('/api/facturacion/codigos-servicio');
  const hoy = fmt.hoy();
  const primeroDelMes = hoy.substring(0, 8) + '01';
  el.innerHTML = html`
    <div class="tarjeta">
      <h2>Exportar asientos contables a FEBBO</h2>
      <p class="ayuda">Genera el archivo de asientos del rango elegido: cada cuota emitida (Debe deudores / Haber la cuenta de cada concepto)
      y cada pago imputado (Debe la cuenta del medio de cobro / Haber deudores). Formato CSV separado por “;”.</p>
      <div class="fila">
        <div class="campo chico"><label>Desde</label><input type="date" id="desde" value="${primeroDelMes}"></div>
        <div class="campo chico"><label>Hasta</label><input type="date" id="hasta" value="${hoy}"></div>
        <button class="btn primario" id="exportar">Descargar archivo</button>
      </div>
      <div id="res" style="margin-top:12px"></div>
    </div>
    <div class="tarjeta">
      <h2>Código de servicio → cuenta contable</h2>
      <p class="ayuda">Imputación contable de cada concepto facturado.</p>
      ${raw(tabla([
        { t: 'Código', v: c => `<b class="mono">${esc(c.codigo)}</b>` },
        { t: 'Descripción', v: c => esc(c.descripcion) },
        { t: 'Tipo', v: c => `<span class="badge">${esc(c.tipo)}</span>` },
        { t: 'Cuenta', v: c => `<span class="mono">${esc(c.cuentaCodigo)}</span> ${esc(c.cuentaNombre)}` },
        { t: 'Importe fijo', cls: 'num', v: c => c.importeFijo ? fmt.moneda(c.importeFijo) : '—' },
        { t: 'Se suma a la cuota', v: c => c.incluirEnCuota ? '<span class="badge ok">Sí, por integrante</span>' : '—' }
      ], codigos))}
    </div>`;
  $('#exportar', el).addEventListener('click', e => conBoton(e.target, async () => {
    try {
      const h = await api.descargar('/api/facturacion/asientos/exportar', { desde: $('#desde', el).value, hasta: $('#hasta', el).value });
      $('#res', el).innerHTML = html`<div class="cinta ok">Archivo descargado: ${h.get('X-Asientos')} asientos ·
        Debe ${fmt.moneda(h.get('X-Total-Debe'))} · Haber ${fmt.moneda(h.get('X-Total-Haber'))}
        ${raw(h.get('X-Total-Debe') === h.get('X-Total-Haber') ? ' · <b>balanceado</b>' : '')}</div>`;
      toast('Asientos exportados', 'ok');
    } catch (er) { error(er); }
  }));
}
