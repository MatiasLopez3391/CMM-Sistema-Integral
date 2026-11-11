import { api, fmt } from '../api.js';
import { html, raw, esc, $, $$, tabla, modal, datosForm, toast, error, cargando, conBoton } from '../ui.js';

export async function render(el) {
  cargando(el);
  const lista = await api.get('/api/cobertura/derivadoras');
  el.innerHTML = html`
    <div class="tarjeta">
      <div class="tarjeta-titulo"><h2>Derivadoras (obra social de origen)</h2><button class="btn primario" id="nuevo">+ Nueva derivadora</button></div>
      <p class="ayuda">Obras sociales que derivan afiliados monotributistas. Se vinculan al afiliado como "obra social de origen".</p>
      ${raw(tabla([
        { t: 'Nombre', v: d => `<b>${esc(d.nombre)}</b>` },
        { t: 'CUIT', v: d => d.cuit ? `<span class="mono">${esc(d.cuit)}</span>` : '<span class="suave">—</span>' },
        { t: 'Convenio', v: d => esc(d.convenio || '—') },
        { t: 'Vigencia', v: d => d.vigenteDesde ? `${fmt.fecha(d.vigenteDesde)} – ${d.vigenteHasta ? fmt.fecha(d.vigenteHasta) : 'sin fin'}` : '—' },
        { t: 'Afiliados activos', cls: 'num', v: d => d.afiliados },
        { t: 'Estado', v: d => d.activa ? '<span class="badge ok">Activa</span>' : '<span class="badge">Inactiva</span>' }
      ], lista, { clic: true }))}
    </div>`;
  $('#nuevo', el).addEventListener('click', () => editar(null));
  $$('tbody tr', el).forEach(tr => tr.addEventListener('click', () => editar(lista[tr.dataset.i])));
}

function editar(d) {
  const m = modal(d ? d.nombre : 'Nueva derivadora', `
    <form class="form-grid">
      <div class="campo"><label>Nombre *</label><input name="nombre" value="${esc(d?.nombre)}" required></div>
      <div class="campo"><label>CUIT</label><input name="cuit" value="${esc(d?.cuit)}" placeholder="30-12345678-9"></div>
      <div class="campo" style="grid-column:1/-1"><label>Convenio</label><input name="convenio" value="${esc(d?.convenio)}"></div>
      <div class="campo"><label>Vigente desde</label><input type="date" name="vigenteDesde" value="${esc(d?.vigenteDesde)}"></div>
      <div class="campo"><label>Vigente hasta</label><input type="date" name="vigenteHasta" value="${esc(d?.vigenteHasta)}"></div>
      ${d ? `<label class="campo" style="flex-direction:row;align-items:center;gap:6px"><input type="checkbox" name="activa" ${d.activa ? 'checked' : ''}> Activa</label>` : ''}
    </form>
    <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn primario" id="ok">Guardar</button></div>`);
  $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
    try {
      const datos = datosForm($('form', m.el));
      if (!d) datos.activa = true;
      if (d) await api.put(`/api/cobertura/derivadoras/${d.id}`, datos); else await api.post('/api/cobertura/derivadoras', datos);
      m.cerrar(); toast('Derivadora guardada', 'ok'); render(document.getElementById('vista'));
    } catch (er) { error(er); }
  }));
}
