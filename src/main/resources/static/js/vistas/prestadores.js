import { api, fmt } from '../api.js';
import { html, raw, esc, $, $$, tabla, modal, datosForm, toast, error, opciones, cargando, conBoton } from '../ui.js';

let prestadorSel = null;

export async function render(el) {
  cargando(el);
  const prestadores = await api.get('/api/cobertura/prestadores');
  if (!prestadorSel || !prestadores.some(p => p.id === prestadorSel)) prestadorSel = prestadores[0]?.id ?? null;
  el.innerHTML = html`
    <div class="tarjeta">
      <div class="tarjeta-titulo">
        <h2>Prestadores</h2>
        <div class="fila"><button class="btn" id="nuevaPractica">+ Práctica</button><button class="btn primario" id="nuevo">+ Prestador</button></div>
      </div>
      <p class="ayuda">Valores convenidos por práctica, con vigencia. El coseguro del afiliado se calcula según la cobertura de su plan
      (valor × (1 − % de cobertura)).</p>
      <div class="fila">
        <div class="campo"><label>Prestador</label><select id="selPrestador">${raw(opciones(prestadores, prestadorSel, { texto: p => `${p.nombre} (${p.tipo})` }))}</select></div>
        <div class="campo chico"><label>Valores vigentes al</label><input type="date" id="fecha" value="${fmt.hoy()}"></div>
        <button class="btn" id="editarPrestador">Editar prestador</button>
      </div>
    </div>
    <div class="tarjeta">
      <div class="tarjeta-titulo">
        <h2>Valores convenidos</h2>
        <div class="fila"><button class="btn" id="cargarValor">+ Valor</button><button class="btn primario" id="importar">Importar planilla (Excel/CSV)</button></div>
      </div>
      <div id="valores"></div>
    </div>
    <div class="tarjeta">
      <h2>Simulador de coseguro por plan</h2>
      <p class="ayuda">Elegí una práctica para ver cuánto paga la obra social y cuánto el afiliado en cada plan.</p>
      <div class="fila"><div class="campo"><label>Práctica</label><select id="selPractica"></select></div></div>
      <div id="simulacion" style="margin-top:12px"></div>
    </div>`;

  const cargarValores = async () => {
    if (!prestadorSel) { $('#valores', el).innerHTML = '<div class="vacio">No hay prestadores cargados.</div>'; return; }
    try {
      const valores = await api.get(`/api/cobertura/prestadores/${prestadorSel}/valores`, { fecha: $('#fecha', el).value });
      $('#valores', el).innerHTML = tabla([
        { t: 'Código CMM', v: v => `<span class="mono">${esc(v.codigoCmm)}</span>` },
        { t: 'Nomenclador', v: v => `<span class="mono suave">${esc(v.codigoNomenclador || '')}</span>` },
        { t: 'Práctica', v: v => esc(v.descripcion) },
        { t: 'Rubro', v: v => `<span class="badge">${esc(v.rubro)}</span>` },
        { t: 'Vigente desde', v: v => fmt.fecha(v.vigenteDesde) },
        { t: 'Valor', cls: 'num', v: v => `<b>${fmt.moneda(v.importe)}</b>` },
        { t: 'Anterior', cls: 'num', v: v => v.importeAnterior ? fmt.moneda(v.importeAnterior) : '—' },
        { t: 'Variación', cls: 'num', v: v => v.variacionPorcentaje !== null ? fmt.pct(v.variacionPorcentaje) : '—' }
      ], valores, { vacio: 'El prestador no tiene valores vigentes a esa fecha.' });
      $('#selPractica', el).innerHTML = opciones(valores, $('#selPractica', el).value, { valor: 'practicaId', texto: v => `${v.codigoCmm} · ${v.descripcion}`, vacio: 'Seleccionar práctica…' });
    } catch (e) { error(e); }
  };

  const simular = async () => {
    const practicaId = $('#selPractica', el).value;
    if (!practicaId) { $('#simulacion', el).innerHTML = ''; return; }
    try {
      const filas = await api.get('/api/cobertura/coseguro/simulador', { prestadorId: prestadorSel, practicaId, fecha: $('#fecha', el).value });
      $('#simulacion', el).innerHTML = tabla([
        { t: 'Plan', v: c => `<b class="mono">${esc(c.planCodigo)}</b> ${esc(c.planNombre)}` },
        { t: 'Valor convenido', cls: 'num', v: c => fmt.moneda(c.valorConvenido) },
        { t: 'Cobertura', cls: 'num', v: c => fmt.pct(c.porcentajeCobertura) },
        { t: 'Paga la obra social', cls: 'num', v: c => fmt.moneda(c.aCargoEntidad) },
        { t: 'Coseguro afiliado', cls: 'num', v: c => `<b>${fmt.moneda(c.coseguroAfiliado)}</b>` }
      ], filas) + `<p class="suave" style="margin-top:8px">Valor vigente desde ${fmt.fecha(filas[0]?.valorVigenteDesde)} · rubro ${esc(filas[0]?.rubro)}</p>`;
    } catch (e) { error(e); $('#simulacion', el).innerHTML = ''; }
  };

  $('#selPrestador', el).addEventListener('change', e => { prestadorSel = Number(e.target.value); cargarValores().then(simular); });
  $('#fecha', el).addEventListener('change', () => cargarValores().then(simular));
  $('#selPractica', el).addEventListener('change', simular);
  $('#nuevo', el).addEventListener('click', () => editarPrestador(null));
  $('#editarPrestador', el).addEventListener('click', () => editarPrestador(prestadores.find(p => p.id === prestadorSel)));
  $('#nuevaPractica', el).addEventListener('click', nuevaPractica);
  $('#cargarValor', el).addEventListener('click', () => cargarValor(cargarValores));
  $('#importar', el).addEventListener('click', () => importar(cargarValores));
  await cargarValores();
  const primera = $('#selPractica', el).options[1];
  if (primera) { $('#selPractica', el).value = primera.value; simular(); }
}

function editarPrestador(p) {
  const m = modal(p ? p.nombre : 'Nuevo prestador', `
    <form class="form-grid">
      <div class="campo"><label>Nombre *</label><input name="nombre" value="${esc(p?.nombre)}" required></div>
      <div class="campo"><label>CUIT</label><input name="cuit" value="${esc(p?.cuit)}"></div>
      <div class="campo"><label>Tipo *</label><select name="tipo">${opciones(['CLINICA', 'INSTITUCION', 'PROFESIONAL', 'LABORATORIO', 'FARMACIA'].map(v => ({ id: v, nombre: v })), p?.tipo || 'CLINICA')}</select></div>
      <div class="campo"><label>Email de liquidaciones</label><input type="email" name="email" value="${esc(p?.email)}"></div>
      ${p ? `<label class="campo" style="flex-direction:row;align-items:center;gap:6px"><input type="checkbox" name="activo" ${p.activo ? 'checked' : ''}> Activo</label>` : ''}
    </form>
    <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn primario" id="ok">Guardar</button></div>`);
  $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
    try {
      const d = datosForm($('form', m.el));
      if (!p) d.activo = true;
      const r = p ? await api.put(`/api/cobertura/prestadores/${p.id}`, d) : await api.post('/api/cobertura/prestadores', d);
      prestadorSel = r.id;
      m.cerrar(); toast('Prestador guardado', 'ok'); render(document.getElementById('vista'));
    } catch (er) { error(er); }
  }));
}

function nuevaPractica() {
  const m = modal('Nueva práctica', `
    <form class="form-grid">
      <div class="campo"><label>Código CMM *</label><input name="codigoCmm" required></div>
      <div class="campo"><label>Código nomenclador</label><input name="codigoNomenclador"></div>
      <div class="campo" style="grid-column:1/-1"><label>Descripción *</label><input name="descripcion" required></div>
      <div class="campo"><label>Rubro *</label><select name="rubro">${opciones(['CONSULTA', 'ENFERMERIA', 'LABORATORIO', 'IMAGENES', 'MEDICAMENTOS', 'INTERNACION', 'ODONTOLOGIA', 'OTROS'].map(v => ({ id: v, nombre: v })), 'CONSULTA')}</select></div>
      <label class="campo" style="flex-direction:row;align-items:center;gap:6px"><input type="checkbox" name="requiereAuditoria"> Requiere auditoría médica</label>
    </form>
    <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn primario" id="ok">Guardar</button></div>`);
  $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
    try { await api.post('/api/cobertura/practicas', datosForm($('form', m.el))); m.cerrar(); toast('Práctica creada', 'ok'); } catch (er) { error(er); }
  }));
}

async function cargarValor(recargar) {
  try {
    const practicas = await api.get('/api/cobertura/practicas');
    const m = modal('Cargar valor convenido', `
      <form class="form-grid">
        <div class="campo" style="grid-column:1/-1"><label>Práctica *</label><select name="practicaId">${opciones(practicas, null, { texto: p => `${p.codigoCmm} · ${p.descripcion}` })}</select></div>
        <div class="campo"><label>Vigente desde *</label><input type="date" name="vigenteDesde" value="${fmt.hoy().substring(0, 8)}01" required></div>
        <div class="campo"><label>Valor *</label><input name="importe" data-tipo="num" required></div>
      </form>
      <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn primario" id="ok">Guardar</button></div>`);
    $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
      try {
        const d = datosForm($('form', m.el)); d.practicaId = Number(d.practicaId);
        await api.post(`/api/cobertura/prestadores/${prestadorSel}/valores`, d);
        m.cerrar(); toast('Valor guardado', 'ok'); recargar();
      } catch (er) { error(er); }
    }));
  } catch (e) { error(e); }
}

function importar(recargar) {
  const m = modal('Importar planilla del prestador', `
    <p class="ayuda">Acepta Excel (.xlsx/.xls) o CSV. Se detectan las columnas por nombre: <b>CMM</b> (o Código CMM), <b>Código</b>/<b>Nomenclador</b>,
    <b>Detalle práctica</b>, y el valor (<b>Valor</b>, <b>Importe</b> o la columna "Valores …" más a la derecha). Las columnas "a cargo de…" se ignoran.
    <a href="ejemplos/planilla-prestador-ejemplo.xlsx">Descargar planilla modelo</a>.</p>
    <form class="form-grid">
      <div class="campo"><label>Vigente desde *</label><input type="date" name="vigenteDesde" value="${fmt.hoy().substring(0, 8)}01" required></div>
      <div class="campo" style="grid-column:1/-1"><label>Archivo *</label><input type="file" name="archivo" accept=".xlsx,.xls,.csv,.txt" required></div>
    </form>
    <div id="resultadoImp"></div>
    <div class="acciones"><button class="btn" data-cerrar>Cerrar</button><button class="btn primario" id="ok">Importar</button></div>`);
  $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
    try {
      const f = $('form', m.el);
      if (!f.archivo.files[0]) throw new Error('Seleccione un archivo.');
      const fd = new FormData(); fd.append('archivo', f.archivo.files[0]);
      const r = await api.multipart(`/api/cobertura/prestadores/${prestadorSel}/valores/importar`, fd, { vigenteDesde: f.vigenteDesde.value });
      $('#resultadoImp', m.el).innerHTML = html`<div class="cinta ok">${r.filasLeidas} filas leídas · ${r.valoresCreados} valores nuevos ·
        ${r.valoresActualizados} actualizados · ${r.practicasCreadas} prácticas nuevas</div>
        ${raw(r.advertencias.length ? `<div class="cinta aviso">${r.advertencias.map(esc).join('<br>')}</div>` : '')}`;
      recargar();
    } catch (er) { error(er); }
  }));
}
