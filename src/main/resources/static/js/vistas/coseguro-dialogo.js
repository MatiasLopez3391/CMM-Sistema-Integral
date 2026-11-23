// Carga manual de un coseguro que se cobra en la cuota siguiente (T42).
// Se usa desde Cuotas › Coseguros y desde la cuenta corriente del afiliado.
import { api, fmt } from '../api.js';
import { esc, $, tabla, modal, toast, error, opciones, conBoton, badgeBloqueado } from '../ui.js';

const mesSiguiente = fecha => {
  let [a, m] = fecha.substring(0, 7).split('-').map(Number);
  m++; if (m > 12) { m = 1; a++; }
  return `${a}-${String(m).padStart(2, '0')}`;
};

/**
 * @param {object} opts
 * @param {Array<{id,nroAfiliado,apellidoNombre}>} [opts.grupo] integrantes ya conocidos (ficha del afiliado)
 * @param {Function} [opts.alTerminar]
 */
export function dialogoCoseguro({ grupo = null, alTerminar } = {}) {
  const hoy = fmt.hoy();
  const m = modal('Cargar coseguro a la próxima cuota', `
    <form class="form-grid" id="fCos">
      ${grupo ? `
        <div class="campo" style="grid-column:1/-1"><label>Integrante *</label>
          <select name="afiliadoId">${opciones(grupo, null, { texto: g => `${g.nroAfiliado} · ${g.apellidoNombre}` })}</select></div>` : `
        <div class="campo" style="grid-column:1/-1"><label>Buscar afiliado (número, apellido o documento) *</label>
          <div class="fila"><input id="qAf" placeholder="Ej.: 131090000100 o Gómez" style="flex:1"><button type="button" class="btn" id="bAf">Buscar</button></div>
          <input type="hidden" name="afiliadoId"><div id="resAf" style="margin-top:6px"></div></div>`}
      <div class="campo"><label>Fecha de la orden *</label><input type="date" name="fecha" value="${hoy}" max="${hoy}" required></div>
      <div class="campo"><label>Cuota en la que se cobra *</label><input type="month" name="periodoDestino" value="${mesSiguiente(hoy)}" required></div>
      <div class="campo" style="grid-column:1/-1"><label>Cómo se obtiene el importe</label>
        <div class="fila">
          <label style="display:inline-flex;gap:6px"><input type="radio" name="modo" value="calcular" checked> Calcular según la cobertura del plan</label>
          <label style="display:inline-flex;gap:6px"><input type="radio" name="modo" value="manual"> Ingresar el importe</label>
        </div></div>
      <div id="bCalc" class="form-grid" style="grid-column:1/-1;margin:0">
        <div class="campo"><label>Prestador</label><select id="prest"><option value="">Cargando…</option></select></div>
        <div class="campo"><label>Práctica</label><select id="prac" disabled><option value="">Elija un prestador</option></select></div>
        <div style="grid-column:1/-1"><button type="button" class="btn" id="calc">Calcular coseguro</button> <span id="resCalc" class="suave"></span></div>
      </div>
      <div class="campo" style="grid-column:1/-1"><label>Concepto *</label><input name="concepto" maxlength="200" placeholder="Ej.: Consulta especialista - Clínica Santa Rosa" required></div>
      <div class="campo"><label>Importe del coseguro *</label><input name="importe" data-tipo="num" placeholder="0,00" required></div>
    </form>
    <p class="ayuda">El coseguro queda pendiente y se suma solo a la cuota del período elegido, con el nombre del integrante.
    Si esa cuota ya se generó, entra en la siguiente.</p>
    <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn primario" id="ok">Guardar</button></div>`);
  const f = $('#fCos', m.el);

  f.fecha.addEventListener('change', () => { if (f.fecha.value) f.periodoDestino.value = mesSiguiente(f.fecha.value); });

  if (!grupo) {
    const buscar = async () => {
      try {
        const r = await api.get('/api/afiliados', { q: $('#qAf', m.el).value, estado: 'ACTIVO', tamanio: 8 });
        $('#resAf', m.el).innerHTML = tabla([
          { t: '', v: a => `<input type="radio" name="selAf" value="${a.id}">` },
          { t: 'Nro. afiliado', v: a => `<span class="mono">${esc(a.nroAfiliado)}</span>` },
          { t: 'Apellido y nombre', v: a => esc(a.apellidoNombre) + ' ' + badgeBloqueado(a.bloqueado) },
          { t: 'Parentesco', v: a => esc(a.parentesco) },
          { t: 'Plan', v: a => esc(a.planCodigo) }
        ], r.contenido, { vacio: 'Sin resultados.' });
        m.el.querySelectorAll('[name=selAf]').forEach(x => x.addEventListener('change', () => { f.afiliadoId.value = x.value; }));
      } catch (e) { error(e); }
    };
    $('#bAf', m.el).addEventListener('click', buscar);
    $('#qAf', m.el).addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); buscar(); } });
  }

  // Cálculo según cobertura: prestador -> prácticas con valor vigente -> coseguro del plan del afiliado
  const bloqueCalc = $('#bCalc', m.el);
  f.querySelectorAll('[name=modo]').forEach(r => r.addEventListener('change', () => {
    bloqueCalc.hidden = f.modo.value !== 'calcular';
  }));
  let valores = [];
  api.get('/api/cobertura/prestadores').then(ps => {
    $('#prest', m.el).innerHTML = opciones(ps.filter(p => p.activo), null, { vacio: 'Elija un prestador' });
  }).catch(error);
  $('#prest', m.el).addEventListener('change', async e => {
    const prac = $('#prac', m.el);
    prac.disabled = true;
    if (!e.target.value) { prac.innerHTML = '<option value="">Elija un prestador</option>'; return; }
    try {
      valores = await api.get(`/api/cobertura/prestadores/${e.target.value}/valores`, { fecha: f.fecha.value });
      prac.innerHTML = opciones(valores, null, { valor: 'practicaId', texto: v => `${v.codigoCmm} · ${v.descripcion} (${fmt.moneda(v.importe)})`, vacio: 'Elija una práctica' });
      prac.disabled = false;
    } catch (er) { error(er); }
  });
  $('#calc', m.el).addEventListener('click', e => conBoton(e.target, async () => {
    try {
      if (!f.afiliadoId.value) throw new Error('Elija primero el afiliado.');
      const prestadorId = $('#prest', m.el).value, practicaId = $('#prac', m.el).value;
      if (!prestadorId || !practicaId) throw new Error('Elija el prestador y la práctica.');
      const r = await api.post('/api/facturacion/coseguros/calcular', {
        afiliadoId: Number(f.afiliadoId.value), prestadorId: Number(prestadorId), practicaId: Number(practicaId), fecha: f.fecha.value
      });
      const practica = valores.find(v => String(v.practicaId) === practicaId);
      const prestador = $('#prest', m.el).selectedOptions[0].textContent;
      f.importe.value = String(r.coseguroAfiliado).replace('.', ',');
      if (!f.concepto.value) f.concepto.value = `${practica.descripcion} - ${prestador}`;
      $('#resCalc', m.el).textContent = `Plan ${r.planCodigo}: valor ${fmt.moneda(r.valorConvenido)}, cobertura ${fmt.pct(r.porcentajeCobertura)} → coseguro ${fmt.moneda(r.coseguroAfiliado)}`;
    } catch (er) { error(er); }
  }));

  $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
    try {
      if (!f.afiliadoId.value) throw new Error('Elija el afiliado.');
      const txt = String(f.importe.value).trim();
      const importe = Number(txt.includes(',') ? txt.replace(/\./g, '').replace(',', '.') : txt);
      if (!(importe > 0)) throw new Error('Ingrese un importe mayor a cero.');
      const r = await api.post('/api/facturacion/coseguros', {
        afiliadoId: Number(f.afiliadoId.value), fecha: f.fecha.value, importe,
        concepto: f.concepto.value.trim(), periodoDestino: f.periodoDestino.value
      });
      m.cerrar();
      toast(`Coseguro de ${fmt.moneda(r.importe)} cargado a la cuota ${fmt.periodo(r.periodoDestino)}`, 'ok');
      alTerminar && alTerminar();
    } catch (er) { error(er); }
  }));
}

/** Pide el motivo y anula un coseguro pendiente. */
export function dialogoAnularCoseguro(cargo, alTerminar) {
  const m = modal('Anular coseguro', `
    <p>${esc(cargo.afiliadoNombre)} · ${esc(cargo.concepto)} · <b>${fmt.moneda(cargo.importe)}</b></p>
    <div class="campo"><label>Motivo *</label><input id="motivo" maxlength="200" placeholder="Ej.: orden anulada por el afiliado"></div>
    <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn peligro" id="ok">Anular</button></div>`);
  $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
    try {
      await api.post(`/api/facturacion/coseguros/${cargo.id}/anular`, { motivo: $('#motivo', m.el).value });
      m.cerrar(); toast('Coseguro anulado', 'ok'); alTerminar && alTerminar();
    } catch (er) { error(er); }
  }));
}
