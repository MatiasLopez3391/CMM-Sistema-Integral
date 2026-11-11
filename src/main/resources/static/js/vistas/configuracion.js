import { api } from '../api.js';
import { html, raw, esc, $, $$, tabla, modal, datosForm, toast, error, cargando, conBoton, opciones } from '../ui.js';

export async function render(el) {
  cargando(el);
  const [parametros, layouts] = await Promise.all([api.get('/api/facturacion/parametros'), api.get('/api/cobranza/layouts')]);
  el.innerHTML = html`
    <div class="tarjeta">
      <h2>Parámetros de facturación</h2>
      <p class="ayuda">Valores utilizados en la generación de cuotas y el control de morosos. Cada modificación queda registrada.</p>
      ${raw(tabla([
        { t: 'Parámetro', v: p => `<span class="mono">${esc(p.clave)}</span>` },
        { t: 'Descripción', v: p => esc(p.descripcion) },
        { t: 'Valor', v: p => `<input class="valor" data-clave="${esc(p.clave)}" value="${esc(p.valor)}" style="width:140px">` },
        { t: '', cls: 'derecha', v: p => `<button class="btn chico" data-guardar="${esc(p.clave)}">Guardar</button>` }
      ], parametros))}
    </div>
    <div class="tarjeta">
      <h2>Diseños de archivo de cobranza</h2>
      <p class="ayuda">Posición de cada campo en los archivos que envían los entes de cobro.
      Ancho fijo: posición de carácter (desde 1) y largo. Delimitado: número de columna.</p>
      ${raw(tabla([
        { t: 'Ente', v: l => esc(l.medio) },
        { t: 'Diseño', v: l => esc(l.nombre) },
        { t: 'Formato', v: l => esc(l.formato) + (l.delimitador ? ` (“${esc(l.delimitador)}”)` : '') },
        { t: 'Afiliado', v: l => `${l.afiliadoPos}${l.afiliadoLargo ? '/' + l.afiliadoLargo : ''}` },
        { t: 'Fecha', v: l => `${l.fechaPos}${l.fechaLargo ? '/' + l.fechaLargo : ''} <span class="mono">${esc(l.fechaFormato)}</span>` },
        { t: 'Importe', v: l => `${l.importePos}${l.importeLargo ? '/' + l.importeLargo : ''}${l.importeDecimalesImpl ? ` (${l.importeDecimalesImpl} dec. impl.)` : ''}` },
        { t: '', cls: 'derecha', v: () => '<button class="btn chico">Editar</button>' }
      ], layouts))}
    </div>`;

  $$('[data-guardar]', el).forEach(b => b.addEventListener('click', () => conBoton(b, async () => {
    const clave = b.dataset.guardar;
    try {
      await api.put(`/api/facturacion/parametros/${encodeURIComponent(clave)}`, { valor: $(`.valor[data-clave="${clave}"]`, el).value });
      toast('Parámetro actualizado', 'ok');
    } catch (e) { error(e); }
  })));
  $$('tbody tr', el.querySelectorAll('.tarjeta')[1]).forEach(tr => tr.querySelector('button').addEventListener('click', () => editarLayout(layouts[tr.dataset.i], el)));
}

function editarLayout(l, el) {
  const num = (n, v, etq) => `<div class="campo chico"><label>${etq}</label><input name="${n}" data-tipo="num" value="${v ?? ''}"></div>`;
  const m = modal(`Diseño: ${l.nombre}`, `
    <form class="form-grid">
      <div class="campo" style="grid-column:1/-1"><label>Nombre</label><input name="nombre" value="${esc(l.nombre)}"></div>
      <div class="campo"><label>Formato</label><select name="formato">${opciones([{ id: 'ANCHO_FIJO', nombre: 'Ancho fijo' }, { id: 'DELIMITADO', nombre: 'Delimitado' }], l.formato)}</select></div>
      <div class="campo chico"><label>Delimitador</label><input name="delimitador" value="${esc(l.delimitador)}"></div>
      <div class="campo chico"><label>Prefijo de registro</label><input name="prefijoRegistro" value="${esc(l.prefijoRegistro)}"></div>
      ${num('lineasEncabezado', l.lineasEncabezado, 'Líneas de encabezado')}
      ${num('afiliadoPos', l.afiliadoPos, 'Afiliado: posición')}${num('afiliadoLargo', l.afiliadoLargo, 'Afiliado: largo')}
      ${num('fechaPos', l.fechaPos, 'Fecha: posición')}${num('fechaLargo', l.fechaLargo, 'Fecha: largo')}
      <div class="campo chico"><label>Formato de fecha</label><input name="fechaFormato" value="${esc(l.fechaFormato)}"></div>
      ${num('importePos', l.importePos, 'Importe: posición')}${num('importeLargo', l.importeLargo, 'Importe: largo')}
      ${num('importeDecimalesImpl', l.importeDecimalesImpl, 'Decimales implícitos')}
      ${num('referenciaPos', l.referenciaPos, 'Referencia: posición')}${num('referenciaLargo', l.referenciaLargo, 'Referencia: largo')}
      <div class="campo" style="grid-column:1/-1"><label>Observaciones</label><input name="observaciones" value="${esc(l.observaciones)}"></div>
    </form>
    <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn primario" id="ok">Guardar</button></div>`);
  $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
    try {
      const d = { ...l, ...datosForm($('form', m.el)) };
      d.lineasEncabezado = d.lineasEncabezado ?? 0;
      d.importeDecimalesImpl = d.importeDecimalesImpl ?? 0;
      await api.put(`/api/cobranza/layouts/${l.id}`, d);
      m.cerrar(); toast('Diseño guardado', 'ok'); render(el);
    } catch (er) { error(er); }
  }));
}
