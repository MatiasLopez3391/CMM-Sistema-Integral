import { api, fmt } from '../api.js';
import { html, raw, esc, $, $$, tabla, modal, datosForm, toast, error, cargando, conBoton } from '../ui.js';

const RUBROS = ['CONSULTA', 'ENFERMERIA', 'LABORATORIO', 'IMAGENES', 'MEDICAMENTOS', 'INTERNACION', 'ODONTOLOGIA', 'OTROS'];

export async function render(el) {
  cargando(el);
  const planes = await api.get('/api/cobertura/planes');
  el.innerHTML = html`
    <div class="tarjeta">
      <div class="tarjeta-titulo"><h2>Planes y nivel de cobertura</h2><button class="btn primario" id="nuevo">+ Nuevo plan</button></div>
      <p class="ayuda">El nivel de cobertura (70/100, 80/100, 100%) define qué parte de la práctica paga la obra social; el resto es el coseguro del afiliado.
      Las excepciones por rubro permiten, por ejemplo, cubrir medicamentos al 40% en un plan 80/100.</p>
      ${raw(tabla([
        { t: 'Código', v: p => `<b class="mono">${esc(p.codigo)}</b>` },
        { t: 'Nombre', v: p => esc(p.nombre) },
        { t: 'Cobertura general', cls: 'num', v: p => fmt.pct(p.coberturaPorcentaje) },
        { t: 'Coseguro afiliado', cls: 'num', v: p => fmt.pct(100 - p.coberturaPorcentaje) },
        { t: 'Excepciones por rubro', v: p => p.coberturasPorRubro.map(c => `<span class="badge info">${esc(c.rubro)} ${fmt.pct(c.porcentaje)}</span>`).join(' ') || '<span class="suave">—</span>' },
        { t: 'Estado', v: p => p.activo ? '<span class="badge ok">Activo</span>' : '<span class="badge">Inactivo</span>' }
      ], planes, { clic: true }))}
    </div>`;
  $('#nuevo', el).addEventListener('click', () => editar(null));
  $$('tbody tr', el).forEach(tr => tr.addEventListener('click', () => editar(planes[tr.dataset.i])));
}

function filaRubro(r = {}) {
  return `<div class="fila rubro" style="margin-bottom:6px">
    <div class="campo"><select class="r-rubro">${RUBROS.map(x => `<option ${x === r.rubro ? 'selected' : ''}>${x}</option>`).join('')}</select></div>
    <div class="campo chico"><input class="r-pct" data-tipo="num" value="${r.porcentaje ?? ''}" placeholder="% cobertura"></div>
    <button type="button" class="btn chico peligro r-quitar">Quitar</button></div>`;
}

function editar(p) {
  const m = modal(p ? `Plan ${p.codigo}` : 'Nuevo plan', `
    <form class="form-grid">
      <div class="campo"><label>Código *</label><input name="codigo" value="${esc(p?.codigo)}" required></div>
      <div class="campo"><label>Nombre *</label><input name="nombre" value="${esc(p?.nombre)}" required></div>
      <div class="campo"><label>Cobertura general (%) *</label><input name="coberturaPorcentaje" data-tipo="num" value="${p?.coberturaPorcentaje ?? ''}" required></div>
      ${p ? `<label class="campo" style="flex-direction:row;align-items:center;gap:6px"><input type="checkbox" name="activo" ${p.activo ? 'checked' : ''}> Activo</label>` : ''}
    </form>
    <h3>Excepciones por rubro</h3>
    <div id="rubros">${(p?.coberturasPorRubro || []).map(filaRubro).join('')}</div>
    <button type="button" class="btn chico" id="agregarRubro">+ Agregar excepción</button>
    <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn primario" id="ok">Guardar</button></div>`);
  const enlazar = () => $$('.r-quitar', m.el).forEach(b => { b.onclick = () => b.closest('.rubro').remove(); });
  enlazar();
  $('#agregarRubro', m.el).addEventListener('click', () => { $('#rubros', m.el).insertAdjacentHTML('beforeend', filaRubro()); enlazar(); });
  $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
    try {
      const d = datosForm($('form', m.el));
      d.activo = p ? d.activo : true;
      d.coberturasPorRubro = $$('.rubro', m.el).map(r => ({ rubro: $('.r-rubro', r).value, porcentaje: Number($('.r-pct', r).value.replace(',', '.')) }))
        .filter(r => !isNaN(r.porcentaje));
      if (p) await api.put(`/api/cobertura/planes/${p.id}`, d); else await api.post('/api/cobertura/planes', d);
      m.cerrar(); toast('Plan guardado', 'ok'); render(document.getElementById('vista'));
    } catch (er) { error(er); }
  }));
}
