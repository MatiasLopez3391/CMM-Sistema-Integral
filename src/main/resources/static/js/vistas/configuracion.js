import { api, fmt } from '../api.js';
import { html, raw, esc, $, $$, tabla, modal, datosForm, toast, error, cargando, conBoton, opciones } from '../ui.js';

export async function render(el) {
  cargando(el);
  const anio = Number(sessionStorage.getItem('cmm.anioFeriados')) || new Date().getFullYear();
  const [parametros, layouts, feriados] = await Promise.all([
    api.get('/api/facturacion/parametros'), api.get('/api/cobranza/layouts'), api.get('/api/facturacion/feriados', { anio })]);
  el.innerHTML = html`
    <div class="tarjeta">
      <h2>Parámetros de facturación</h2>
      <p class="ayuda">Valores utilizados en la generación de cuotas y el control de morosos. Cada modificación queda registrada.</p>
      ${raw(tabla([
        { t: 'Parámetro', v: p => `<span class="mono">${esc(p.clave)}</span>` },
        { t: 'Descripción', v: p => esc(p.descripcion) },
        { t: 'Valor', v: p => `<input class="valor" data-clave="${esc(p.clave)}" value="${esc(p.valor)}" style="width:140px">` },
        { t: '', cls: 'derecha', v: p => `<button class="btn chico" data-guardar="${esc(p.clave)}">Guardar</button>` }
      ], parametros.filter(p => !p.clave.startsWith('contable.export.') && !p.clave.startsWith('contable.lote.') && p.clave !== 'contable.letra_por_defecto' && !p.clave.startsWith('aviso.'))))}
      <p class="suave">El formato del archivo de asientos se ajusta en Exportación contable y el texto del aviso de deuda, en Morosos.</p>
    </div>
    <div class="tarjeta" id="tFeriados">
      <div class="tarjeta-titulo">
        <h2>Feriados y días no laborables</h2>
        <div class="fila">
          <div class="campo chico"><select id="anioFer">${raw([anio - 1, anio, anio + 1].map(a => `<option ${a === anio ? 'selected' : ''}>${a}</option>`).join(''))}</select></div>
          <button class="btn" id="ferNuevo">Agregar día</button>
          <button class="btn primario" id="ferImportar">Importar ${anio} desde internet</button>
        </div>
      </div>
      <p class="ayuda">Si el día de vencimiento de la cuota cae sábado, domingo o en uno de estos días, vence el día hábil siguiente.</p>
      ${raw(tabla([
        { t: 'Fecha', v: f => fmt.fecha(f.fecha) + ` <span class="suave">${new Date(f.fecha + 'T12:00').toLocaleDateString('es-AR', { weekday: 'long' })}</span>` },
        { t: 'Descripción', v: f => esc(f.descripcion) },
        { t: 'Origen', v: f => ({ INICIAL: 'Calendario oficial', API: 'Importado', MANUAL: 'Carga manual' }[f.origen] || esc(f.origen)) },
        { t: '', cls: 'derecha', v: f => `<button class="btn chico" data-borrar="${f.fecha}">Quitar</button>` }
      ], feriados, { vacio: `No hay feriados cargados para ${anio}.` }))}
    </div>
    <div class="tarjeta" id="tLayouts">
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
  $('#anioFer', el).addEventListener('change', e => { sessionStorage.setItem('cmm.anioFeriados', e.target.value); render(el); });
  $('#ferImportar', el).addEventListener('click', e => conBoton(e.target, async () => {
    try {
      const r = await api.post(`/api/facturacion/feriados/importar?anio=${anio}`);
      toast(`${r.agregados} día(s) agregados; ${r.yaExistian} ya estaban cargados`, 'ok'); render(el);
    } catch (er) { error(er); }
  }));
  $('#ferNuevo', el).addEventListener('click', () => {
    const m = modal('Agregar día no hábil', `
      <form class="form-grid">
        <div class="campo"><label>Fecha *</label><input type="date" name="fecha" required></div>
        <div class="campo"><label>Descripción *</label><input name="descripcion" maxlength="150" placeholder="Ej.: Feriado provincial"></div>
      </form>
      <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn primario" id="ok">Guardar</button></div>`);
    $('#ok', m.el).addEventListener('click', ev => conBoton(ev.target, async () => {
      try { await api.post('/api/facturacion/feriados', datosForm($('form', m.el))); m.cerrar(); toast('Día agregado', 'ok'); render(el); } catch (er) { error(er); }
    }));
  });
  $$('[data-borrar]', el).forEach(b => b.addEventListener('click', () => conBoton(b, async () => {
    if (!confirm(`¿Quitar el ${fmt.fecha(b.dataset.borrar)} de los días no hábiles?`)) return;
    try { await api.del(`/api/facturacion/feriados/${b.dataset.borrar}`); toast('Día quitado', 'ok'); render(el); } catch (er) { error(er); }
  })));
  $$('tbody tr', $('#tLayouts', el)).forEach(tr => tr.querySelector('button').addEventListener('click', () => editarLayout(layouts[tr.dataset.i], el)));
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
      <div class="campo chico"><label>Pie: prefijo (control)</label><input name="piePrefijo" value="${esc(l.piePrefijo)}"></div>
      ${num('pieCantidadPos', l.pieCantidadPos, 'Pie cantidad: posición')}${num('pieCantidadLargo', l.pieCantidadLargo, 'Pie cantidad: largo')}
      ${num('pieTotalPos', l.pieTotalPos, 'Pie total: posición')}${num('pieTotalLargo', l.pieTotalLargo, 'Pie total: largo')}
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
