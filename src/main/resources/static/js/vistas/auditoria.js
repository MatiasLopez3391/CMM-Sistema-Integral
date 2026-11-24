// Buzón del médico auditor (F5): autorizar, autorizar con cambios o rechazar las órdenes pendientes.
import { api, fmt } from '../api.js';
import { html, raw, esc, $, $$, tabla, modal, toast, error, cargando, conBoton } from '../ui.js';
import { badgeOrden, tablaOrdenes, enlazarFilas, verOrden } from './ordenes.js';

export async function render(el) {
  cargando(el);
  const [pendientes, resueltas] = await Promise.all([api.get('/api/auditoria-medica/pendientes'), api.get('/api/auditoria-medica/resueltas')]);
  el.innerHTML = html`
    <div class="tarjeta">
      <h2>Pendientes de auditoría (${pendientes.length})</h2>
      <div class="cinta info">Al autorizar, el coseguro se suma a la cuota del mes siguiente. Puede autorizar menos cantidad o dejar
      una práctica sin autorizar; en ese caso, igual que al rechazar, el motivo es obligatorio y se le informa al afiliado por mail.</div>
      ${raw(tabla([
        { t: 'Número', v: o => `<span class="mono" style="white-space:nowrap">${esc(o.numero)}</span>` },
        { t: 'Emisión', v: o => fmt.fecha(o.fechaEmision) },
        { t: 'Afiliado', v: o => `${esc(o.afiliado)} <span class="suave mono">${esc(o.nroAfiliado)}</span>` },
        { t: 'Prestador', v: o => esc(o.prestador) },
        { t: 'Por qué vino a auditoría', v: o => esc(o.motivoAuditoria || '—') },
        { t: 'Coseguro', cls: 'num', v: o => fmt.moneda(o.totalCoseguro) },
        { t: '', cls: 'derecha', v: o => `<button class="btn chico primario" data-auditar="${o.id}">Auditar</button>` }
      ], pendientes, { vacio: 'No hay órdenes pendientes de auditoría.' }))}
    </div>
    <div class="tarjeta">
      <h2>Resueltas recientemente</h2>
      <div id="resueltas">${raw(tablaOrdenes(resueltas, 'Todavía no resolvió ninguna orden.'))}</div>
    </div>`;
  const recargar = () => render(el).catch(error);
  $$('[data-auditar]', el).forEach(b => b.addEventListener('click', () => auditar(Number(b.dataset.auditar), recargar)));
  enlazarFilas($('#resueltas', el), resueltas, recargar);
}

async function auditar(id, alTerminar) {
  let d;
  try { d = await api.get(`/api/ordenes/${id}`); } catch (e) { error(e); return; }
  const o = d.orden;
  const m = modal(`Auditar la orden ${o.numero}`, `
    <div class="datos">
      <div><div class="dato-etiqueta">Afiliado</div><div class="dato-valor">${esc(o.afiliado)} <span class="suave mono">${esc(o.nroAfiliado)}</span></div></div>
      <div><div class="dato-etiqueta">Plan</div><div class="dato-valor">${esc(d.planCodigo)}</div></div>
      <div><div class="dato-etiqueta">Prestador</div><div class="dato-valor">${esc(o.prestador)}</div></div>
      <div><div class="dato-etiqueta">Emisión</div><div class="dato-valor">${fmt.fecha(o.fechaEmision)} <span class="suave">por ${esc(d.emitidaPor)}</span></div></div>
      <div><div class="dato-etiqueta">Médico que prescribe</div><div class="dato-valor">${esc(d.medicoSolicitante || '—')}</div></div>
      <div><div class="dato-etiqueta">Estado</div><div class="dato-valor">${badgeOrden(o)}</div></div>
    </div>
    ${d.observaciones ? `<p style="margin-top:10px"><b>Indicación:</b> ${esc(d.observaciones)}</p>` : ''}
    <div class="cinta aviso" style="margin-top:10px">Vino a auditoría: ${esc(o.motivoAuditoria || '—')}</div>
    <p><a href="#/afiliados/${o.afiliadoId}/hc">Ver la historia clínica del afiliado</a> · <a href="#/afiliados/${o.afiliadoId}/ordenes">Ver sus órdenes anteriores</a></p>
    <h3 style="margin-top:12px">Prácticas pedidas</h3>
    ${tabla([
      { t: 'Código', v: i => `<span class="mono">${esc(i.codigo)}</span>` },
      { t: 'Práctica', v: i => esc(i.descripcion) + (i.requiereAuditoria ? ' <span class="badge aviso">Requiere auditoría</span>' : '') },
      { t: 'Pedido', cls: 'num', v: i => i.cantidadSolicitada },
      { t: 'Coseguro pedido', cls: 'num', v: i => fmt.moneda(i.coseguro) },
      { t: 'Autoriza', cls: 'num', v: i => `<input type="number" class="autoriza" data-item="${i.id}" min="0" max="${i.cantidadSolicitada}" value="${i.cantidad}" style="width:80px" aria-label="Cantidad autorizada de ${esc(i.descripcion)}">` }
    ], d.items)}
    <p class="ayuda">Para no autorizar una práctica, ponga 0. No se pueden agregar prácticas ni subir cantidades: eso requiere otra orden.</p>
    <div class="campo" style="margin-top:8px"><label>Motivo / comentario <span class="suave">(obligatorio al rechazar o al cambiar cantidades; se le informa al afiliado)</span></label>
      <textarea id="motivo" maxlength="1500"></textarea></div>
    <div class="acciones">
      <button class="btn" data-cerrar>Cancelar</button>
      <button class="btn peligro" id="rechazar">Rechazar</button>
      <button class="btn primario" id="autorizar">Autorizar</button>
    </div>`);
  const decidir = async (decision, btn) => conBoton(btn, async () => {
    try {
      const items = $$('.autoriza', m.el).map(x => ({ itemId: Number(x.dataset.item), cantidad: Number(x.value) }));
      const cambio = d.items.some(i => items.find(x => x.itemId === i.id)?.cantidad !== i.cantidad);
      const r = await api.post(`/api/auditoria-medica/${id}/decision`, {
        decision, comentario: $('#motivo', m.el).value.trim() || null, items: decision === 'AUTORIZADA' && cambio ? items : null
      });
      m.cerrar();
      toast(decision === 'RECHAZADA' ? `Orden ${r.numero} rechazada` : `Orden ${r.numero} autorizada${cambio ? ' con cambios' : ''}`, 'ok');
      alTerminar?.();
    } catch (er) { error(er); }
  });
  $('#autorizar', m.el).addEventListener('click', e => decidir('AUTORIZADA', e.target));
  $('#rechazar', m.el).addEventListener('click', e => decidir('RECHAZADA', e.target));
}

export { verOrden };
