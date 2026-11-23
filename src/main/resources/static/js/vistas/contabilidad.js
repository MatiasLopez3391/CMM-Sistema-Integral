import { api, fmt } from '../api.js';
import { html, raw, esc, $, $$, tabla, toast, error, cargando, conBoton } from '../ui.js';

const ETIQUETAS = {
  'contable.export.columnas': 'Columnas (en orden)',
  'contable.export.separador': 'Separador de columnas',
  'contable.export.formato_fecha': 'Formato de fecha',
  'contable.export.separador_decimal': 'Separador decimal',
  'contable.export.encabezado': 'Primera línea con títulos (SI/NO)',
  'contable.export.extension': 'Extensión del archivo'
};

const NOMBRES_LOTE = {
  'CUOTA.A': 'Factura A (cuota)', 'CUOTA.B': 'Factura B (cuota)',
  'NOTA_DEBITO.A': 'Nota de débito A', 'NOTA_DEBITO.B': 'Nota de débito B',
  'NOTA_CREDITO.A': 'Nota de crédito A', 'NOTA_CREDITO.B': 'Nota de crédito B',
  PAGO: 'Cobranzas (pagos imputados)'
};

export async function render(el) {
  cargando(el);
  const [codigos, formato, parametros] = await Promise.all([
    api.get('/api/facturacion/codigos-servicio'), api.get('/api/facturacion/asientos/formato'), api.get('/api/facturacion/parametros')]);
  const hoy = fmt.hoy();
  const primeroDelMes = hoy.substring(0, 8) + '01';
  const params = parametros.filter(p => ETIQUETAS[p.clave]);
  el.innerHTML = html`
    <div class="tarjeta">
      <h2>Exportar asientos al sistema contable</h2>
      <p class="ayuda">Genera el archivo de asientos del rango elegido para importarlo en el sistema contable: cada cuota o nota de débito
      (Debe deudores / Haber la cuenta de cada concepto), cada nota de crédito (asiento inverso) y cada pago imputado
      (Debe la cuenta del medio de cobro / Haber deudores). El formato del archivo se ajusta abajo.</p>
      <div class="fila">
        <div class="campo chico"><label>Desde</label><input type="date" id="desde" value="${primeroDelMes}"></div>
        <div class="campo chico"><label>Hasta</label><input type="date" id="hasta" value="${hoy}"></div>
        <div class="campo chico"><label>Lote</label><select id="lote"><option value="">Todos</option>
          ${raw([...new Set(Object.values(formato.lotes).filter(v => v && v !== '-'))].sort((a, b) => a - b)
            .map(v => `<option value="${esc(v)}">Lote ${esc(v)}</option>`).join(''))}</select></div>
        <button class="btn primario" id="exportar">Descargar archivo</button>
      </div>
      <div id="res" style="margin-top:12px"></div>
    </div>
    <div class="tarjeta">
      <h2>Formato del archivo</h2>
      <p class="ayuda">Se adapta al sistema contable que use el CMM sin cambiar el programa. Columnas disponibles:
      ${formato.columnasDisponibles.join(', ')} (<span class="mono">importe</span> va sin signo y <span class="mono">dh</span> indica D o H).</p>
      ${raw(tabla([
        { t: 'Opción', v: p => esc(ETIQUETAS[p.clave]) },
        { t: 'Valor', v: p => `<input class="valor" data-clave="${esc(p.clave)}" value="${esc(p.valor)}" style="width:${p.clave.endsWith('columnas') ? 420 : 140}px">` },
        { t: '', cls: 'derecha', v: p => `<button class="btn chico" data-guardar="${esc(p.clave)}">Guardar</button>` }
      ], params))}
      <h3>Lotes del sistema contable</h3>
      <p class="ayuda">Cada tipo de comprobante va en su lote. Los afiliados llevan letra <b>${formato.letra}</b> (consumidor final);
      la letra A se usará cuando la ficha registre la condición frente al IVA del titular. Use <span class="mono">-</span> si ese comprobante no tiene lote.</p>
      ${raw(tabla([
        { t: 'Comprobante', v: k => esc(NOMBRES_LOTE[k] || k) },
        { t: 'Lote', v: k => `<input class="valor" data-clave="contable.lote.${esc(k)}" value="${esc(formato.lotes[k] || '-')}" style="width:80px">` },
        { t: '', cls: 'derecha', v: k => `<button class="btn chico" data-guardar="contable.lote.${esc(k)}">Guardar</button>` }
      ], Object.keys(formato.lotes)))}
      <h3>Así quedan las primeras líneas</h3>
      <pre class="mono" style="background:#f7f9fc;padding:12px;border-radius:6px;overflow:auto">${formato.ejemplo}</pre>
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
      const h = await api.descargar('/api/facturacion/asientos/exportar', { desde: $('#desde', el).value, hasta: $('#hasta', el).value, lote: $('#lote', el).value });
      $('#res', el).innerHTML = html`<div class="cinta ok">Archivo descargado: ${h.get('X-Asientos')} asientos ·
        Debe ${fmt.moneda(h.get('X-Total-Debe'))} · Haber ${fmt.moneda(h.get('X-Total-Haber'))}
        ${raw(h.get('X-Total-Debe') === h.get('X-Total-Haber') ? ' · <b>balanceado</b>' : '')}</div>`;
      toast('Asientos exportados', 'ok');
    } catch (er) { error(er); }
  }));
  $$('[data-guardar]', el).forEach(b => b.addEventListener('click', () => conBoton(b, async () => {
    const clave = b.dataset.guardar;
    try {
      await api.put(`/api/facturacion/parametros/${encodeURIComponent(clave)}`, { valor: $(`.valor[data-clave="${clave}"]`, el).value });
      toast('Formato actualizado', 'ok');
      render(el);
    } catch (er) { error(er); }
  })));
}
