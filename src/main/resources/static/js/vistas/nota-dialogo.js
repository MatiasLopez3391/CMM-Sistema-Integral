// Notas de crédito y débito internas (T46): corrigen errores sin modificar el comprobante original.
import { api, fmt } from '../api.js';
import { esc, $, modal, toast, error, opciones, conBoton } from '../ui.js';

let codigosCache = null;

/**
 * @param {object} o
 * @param {'CREDITO'|'DEBITO'} o.tipo
 * @param {number} o.afiliadoId titular o integrante del grupo
 * @param {string} o.nombre
 * @param {Array} o.comprobantes comprobantes del grupo (para asociar la nota)
 * @param {number} [o.comprobanteId] comprobante preseleccionado
 */
export async function dialogoNota({ tipo, afiliadoId, nombre, comprobantes = [], comprobanteId = null, alTerminar }) {
  try {
    codigosCache = codigosCache || await api.get('/api/facturacion/codigos-servicio');
  } catch (e) { error(e); return; }
  const credito = tipo === 'CREDITO';
  const asociables = comprobantes.filter(c => c.tipo !== 'NOTA_CREDITO' && c.estado !== 'ANULADO');
  const m = modal(`${credito ? 'Nota de crédito' : 'Nota de débito'} · ${nombre}`, `
    <form class="form-grid">
      <div class="campo"><label>Fecha *</label><input type="date" name="fecha" value="${fmt.hoy()}" max="${fmt.hoy()}" required></div>
      <div class="campo"><label>Importe *</label><input name="importe" data-tipo="num" placeholder="0,00" required></div>
      <div class="campo" style="grid-column:1/-1"><label>Comprobante que corrige ${credito ? '' : '(opcional)'}</label>
        <select name="comprobanteAsociadoId">${opciones(asociables, comprobanteId, {
          vacio: credito ? 'Ninguno: se aplica a la deuda más antigua' : 'Ninguno',
          texto: c => `${c.numero} · ${fmt.periodo(c.periodo)} · total ${fmt.moneda(c.total)} · saldo ${fmt.moneda(c.saldo)}`
        })}</select></div>
      <div class="campo" style="grid-column:1/-1"><label>Concepto que corrige *</label>
        <select name="codigoServicioId">${opciones(codigosCache.filter(c => c.activo), null, { texto: c => `${c.codigo} · ${c.descripcion} (cuenta ${c.cuentaCodigo})` })}</select></div>
      <div class="campo" style="grid-column:1/-1"><label>Motivo *</label><input name="motivo" maxlength="200" placeholder="${credito ? 'Ej.: recargo cobrado por error' : 'Ej.: diferencia de cuota no facturada'}"></div>
    </form>
    <p class="ayuda">${credito
      ? 'Cancela deuda del grupo: primero la del comprobante elegido y luego la más antigua. Si no hay deuda, queda a favor para las próximas cuotas.'
      : 'Suma deuda al grupo y vence junto con la próxima cuota. El comprobante original no se modifica.'}
    Se registra en la cuenta corriente y en los asientos contables con la cuenta del concepto elegido.</p>
    <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn primario" id="ok">Emitir nota</button></div>`);
  $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
    try {
      const f = $('form', m.el);
      const txt = f.importe.value.trim();
      const importe = Number(txt.includes(',') ? txt.replace(/\./g, '').replace(',', '.') : txt);
      if (!(importe > 0)) throw new Error('Ingrese un importe mayor a cero.');
      if (!f.motivo.value.trim()) throw new Error('Indique el motivo.');
      const r = await api.post('/api/facturacion/notas', {
        tipo, afiliadoId, fecha: f.fecha.value, importe, motivo: f.motivo.value.trim(),
        codigoServicioId: Number(f.codigoServicioId.value),
        comprobanteAsociadoId: f.comprobanteAsociadoId.value ? Number(f.comprobanteAsociadoId.value) : null
      });
      m.cerrar();
      toast(credito
        ? `${r.numero} emitida: ${fmt.moneda(r.aplicado)} aplicado${r.aFavor > 0 ? `, ${fmt.moneda(r.aFavor)} a favor` : ''}`
        : `${r.numero} emitida por ${fmt.moneda(r.importe)}, vence ${fmt.fecha(r.fechaVencimiento)}`, 'ok');
      alTerminar && alTerminar();
    } catch (er) { error(er); }
  }));
}

export const nombreTipo = t => ({ CUOTA: 'Cuota', NOTA_CREDITO: 'Nota de crédito', NOTA_DEBITO: 'Nota de débito' }[t] || esc(t));
