import { api, fmt } from '../api.js';
import { html, raw, esc, $, $$, tabla, modal, datosForm, toast, error, opciones, badgeEstado, badgeBloqueado, cargando, conBoton } from '../ui.js';

let estadoBusqueda = { q: '', estado: 'ACTIVO', planId: '', bloqueado: '', soloTitulares: false, pagina: 0 };

export async function render(el, params) {
  if (params[0]) return renderFicha(el, Number(params[0]), params[1] || 'datos');
  return renderListado(el);
}

// =====================================================================  LISTADO
async function renderListado(el) {
  const planes = await api.get('/api/cobertura/planes');
  el.innerHTML = html`
    <div class="tarjeta">
      <div class="tarjeta-titulo">
        <h2>Buscar afiliados</h2>
        <button class="btn primario" id="nuevo">+ Nuevo afiliado</button>
      </div>
      <form id="filtros" class="fila">
        <div class="campo"><label>Nro. afiliado, documento o apellido</label><input name="q" value="${estadoBusqueda.q}" placeholder="Ej.: 1310900001, 28455120, Gómez"></div>
        <div class="campo chico"><label>Estado</label><select name="estado">${raw(opciones([{ id: 'ACTIVO', nombre: 'Activos' }, { id: 'BAJA', nombre: 'De baja' }], estadoBusqueda.estado, { vacio: 'Todos' }))}</select></div>
        <div class="campo chico"><label>Plan</label><select name="planId">${raw(opciones(planes, estadoBusqueda.planId, { texto: p => p.codigo + ' ' + p.nombre, vacio: 'Todos' }))}</select></div>
        <div class="campo chico"><label>Bloqueo</label><select name="bloqueado">${raw(opciones([{ id: 'true', nombre: 'Bloqueados' }, { id: 'false', nombre: 'Habilitados' }], estadoBusqueda.bloqueado, { vacio: 'Todos' }))}</select></div>
        <label class="campo chico" style="flex-direction:row;align-items:center;gap:6px"><input type="checkbox" name="soloTitulares" ${raw(estadoBusqueda.soloTitulares ? 'checked' : '')}> Solo titulares</label>
        <button class="btn primario">Buscar</button>
      </form>
    </div>
    <div class="tarjeta"><div id="resultados"></div></div>`;
  $('#filtros', el).addEventListener('submit', e => {
    e.preventDefault();
    const d = datosForm(e.target);
    estadoBusqueda = { q: d.q || '', estado: d.estado || '', planId: d.planId || '', bloqueado: d.bloqueado || '', soloTitulares: d.soloTitulares, pagina: 0 };
    buscar(el);
  });
  $('#nuevo', el).addEventListener('click', () => formularioAfiliado(null));
  buscar(el);
}

async function buscar(el) {
  const cont = $('#resultados', el);
  try {
    const r = await api.get('/api/afiliados', { ...estadoBusqueda, tamanio: 25 });
    cont.innerHTML = tabla([
      { t: 'Nro. afiliado', v: a => `<span class="mono">${esc(a.nroAfiliado)}</span>` },
      { t: 'Apellido y nombre', v: a => esc(a.apellidoNombre) + (a.titularId ? ' <span class="suave">(integrante)</span>' : '') },
      { t: 'Documento', v: a => esc(a.documento) },
      { t: 'Parentesco', v: a => esc(a.parentesco) },
      { t: 'Plan', v: a => `${esc(a.planCodigo)} <span class="suave">${esc(a.planNombre)}</span>` },
      { t: 'Estado', v: a => badgeEstado(a.estado) + ' ' + badgeBloqueado(a.bloqueado) }
    ], r.contenido, { clic: true, vacio: 'No hay afiliados que coincidan con la búsqueda.' })
      + `<div class="paginador">${r.total} resultado(s) · página ${r.pagina + 1} de ${Math.max(r.totalPaginas, 1)}
         <button class="btn chico" id="ant" ${r.pagina === 0 ? 'disabled' : ''}>‹ Anterior</button>
         <button class="btn chico" id="sig" ${r.pagina + 1 >= r.totalPaginas ? 'disabled' : ''}>Siguiente ›</button></div>`;
    $$('tbody tr', cont).forEach(tr => tr.addEventListener('click', () => { location.hash = `#/afiliados/${r.contenido[tr.dataset.i].id}`; }));
    $('#ant', cont).addEventListener('click', () => { estadoBusqueda.pagina--; buscar(el); });
    $('#sig', cont).addEventListener('click', () => { estadoBusqueda.pagina++; buscar(el); });
  } catch (e) { error(e); }
}

// =====================================================================  FICHA
async function renderFicha(el, id, tab) {
  cargando(el);
  const a = await api.get(`/api/afiliados/${id}`);
  const titularId = a.titularId || a.id;
  const esTitular = !a.titularId;
  const activo = a.estado === 'ACTIVO';
  el.innerHTML = html`
    <p><a href="#/afiliados">← Volver al listado</a></p>
    ${raw(a.bloqueado ? `<div class="cinta error"><b>Grupo familiar bloqueado</b>${a.ultimaMarcaBloqueo ? ' · ' + esc(a.ultimaMarcaBloqueo) : ''}. Sigue activo en la obra social pero no puede pedir órdenes.</div>` : '')}
    ${raw(a.estado === 'BAJA' ? `<div class="cinta aviso">Afiliado dado de baja el ${fmt.fecha(a.fechaBaja)}: ${esc(a.motivoBaja)}</div>` : '')}
    ${raw(a.estadoEspecialVigente ? `<div class="cinta info">Estado especial vigente: <b>${esc(a.estadoEspecial)}</b> (${fmt.fecha(a.estadoEspecialDesde)} – ${fmt.fecha(a.estadoEspecialHasta)})</div>` : '')}
    <div class="tarjeta">
      <div class="cabecera-ficha">
        <div>
          <h2>${a.apellido}, ${a.nombre}</h2>
          <div class="suave"><span class="mono">${a.nroAfiliado}</span> · ${a.parentesco}${raw(a.titularNombre ? ` de <a href="#/afiliados/${a.titularId}">${esc(a.titularNombre)}</a>` : '')} · Plan ${a.planCodigo} ${a.planNombre} (${fmt.pct(a.coberturaPorcentaje)})</div>
        </div>
        <div class="fila">
          ${raw(activo ? '<button class="btn" id="editar">Editar</button>' : '')}
          ${raw(activo && esTitular ? '<button class="btn" id="integrante">+ Integrante</button>' : '')}
          ${raw(activo ? (a.bloqueado ? '<button class="btn" id="desbloquear">Desbloquear</button>' : '<button class="btn peligro" id="bloquear">Bloquear</button>') : '')}
          ${raw(activo ? '<button class="btn peligro" id="baja">Dar de baja</button>' : '')}
        </div>
      </div>
    </div>
    <div class="tarjeta">
      <div class="tabs" id="tabs">
        ${raw(['datos:Datos', 'grupo:Grupo familiar', 'cuenta:Cuenta corriente', 'pagos:Pagos', 'hc:Historia clínica', 'bloqueos:Bloqueos', 'auditoria:Auditoría']
          .map(x => { const [k, t] = x.split(':'); return `<button data-tab="${k}" class="${k === tab ? 'activo' : ''}">${t}</button>`; }).join(''))}
      </div>
      <div id="contenidoTab"></div>
    </div>`;

  $$('#tabs button', el).forEach(b => b.addEventListener('click', () => { location.hash = `#/afiliados/${id}/${b.dataset.tab}`; }));
  $('#editar', el)?.addEventListener('click', () => formularioAfiliado(a));
  $('#integrante', el)?.addEventListener('click', () => formularioIntegrante(a));
  $('#bloquear', el)?.addEventListener('click', () => dialogoBloqueo(a, true));
  $('#desbloquear', el)?.addEventListener('click', () => dialogoBloqueo(a, false));
  $('#baja', el)?.addEventListener('click', () => dialogoBaja(a));

  const c = $('#contenidoTab', el);
  const tabs = { datos: tabDatos, grupo: tabGrupo, cuenta: tabCuenta, pagos: tabPagos, hc: tabHistoria, bloqueos: tabBloqueos, auditoria: tabAuditoria };
  try { await (tabs[tab] || tabDatos)(c, a, titularId); } catch (e) { error(e); }
}

function dato(etq, valor) {
  return `<div><div class="dato-etiqueta">${esc(etq)}</div><div class="dato-valor">${valor === null || valor === undefined || valor === '' ? '—' : esc(valor)}</div></div>`;
}

function tabDatos(c, a) {
  c.innerHTML = `<div class="datos">
    ${dato('Documento', a.tipoDocumento + ' ' + a.documento)}${dato('CUIL', a.cuil)}
    ${dato('Fecha de nacimiento', fmt.fecha(a.fechaNacimiento) + ' (' + a.edad + ' años)')}${dato('Sexo', a.sexo)}
    ${dato('Email', a.email)}${dato('Teléfono', a.telefono)}${dato('Domicilio', [a.domicilio, a.localidad].filter(Boolean).join(', '))}
    ${dato('Área', a.areaNombre)}${dato('Sector', a.sectorNombre)}${dato('Obra social de origen (derivadora)', a.derivadoraNombre)}
    ${dato('Vía de cobro', a.viaCobro)}${dato('Carencia', a.carencia)}${dato('Estado especial', a.estadoEspecial)}
    ${dato('Fecha de alta', fmt.fecha(a.fechaAlta))}${dato('Estado', a.estado)}
    ${dato('Última modificación', a.modificadoEl ? fmt.fechaHora(a.modificadoEl) + ' por ' + a.modificadoPor : fmt.fechaHora(a.creadoEl) + ' por ' + a.creadoPor)}
  </div>`;
}

function tabGrupo(c, a) {
  c.innerHTML = tabla([
    { t: 'Nro. afiliado', v: x => `<span class="mono">${esc(x.nroAfiliado)}</span>` },
    { t: 'Apellido y nombre', v: x => esc(x.apellidoNombre) },
    { t: 'Parentesco', v: x => esc(x.parentesco) },
    { t: 'Documento', v: x => esc(x.documento) },
    { t: 'Estado', v: x => badgeEstado(x.estado) }
  ], a.grupoFamiliar, { clic: true });
  $$('tbody tr', c).forEach(tr => tr.addEventListener('click', () => { location.hash = `#/afiliados/${a.grupoFamiliar[tr.dataset.i].id}`; }));
}

async function tabCuenta(c, a, titularId) {
  const ec = await api.get(`/api/facturacion/cuenta-corriente/${titularId}`);
  c.innerHTML = html`
    <div class="grid-kpi">
      <div class="kpi"><div class="kpi-etiqueta">Saldo actual</div><div class="kpi-valor">${fmt.moneda(ec.saldoFinal)}</div><div class="kpi-nota">positivo = adeuda</div></div>
      <div class="kpi ${raw(ec.deudaVencida > 0 ? 'alerta' : '')}"><div class="kpi-etiqueta">Deuda vencida</div><div class="kpi-valor">${fmt.moneda(ec.deudaVencida)}</div></div>
    </div>
    <div class="fila" style="justify-content:space-between;margin-bottom:10px">
      <span class="suave">Movimientos del ${fmt.fecha(ec.desde)} al ${fmt.fecha(ec.hasta)} · Saldo inicial ${fmt.moneda(ec.saldoInicial)}</span>
      ${raw(a.estado === 'ACTIVO' ? '<button class="btn primario" id="pagar">Registrar pago en ventanilla</button>' : '')}
    </div>
    ${raw(tabla([
      { t: 'Fecha', v: m => fmt.fecha(m.fecha) },
      { t: 'Concepto', v: m => esc(m.concepto) + (m.comprobanteId ? ` <a href="#" data-comp="${m.comprobanteId}">ver</a>` : '') },
      { t: 'Debe', cls: 'num', v: m => m.debe > 0 ? fmt.moneda(m.debe) : '' },
      { t: 'Haber', cls: 'num', v: m => m.haber > 0 ? fmt.moneda(m.haber) : '' },
      { t: 'Saldo', cls: 'num', v: m => fmt.moneda(m.saldo) }
    ], ec.movimientos, { vacio: 'Sin movimientos en el período.' }))}
    <h3>Comprobantes</h3>
    ${raw(tabla([
      { t: 'Número', v: x => `<a href="#" data-comp="${x.id}" class="mono">${esc(x.numero)}</a>` },
      { t: 'Período', v: x => fmt.periodo(x.periodo) },
      { t: 'Vencimiento', v: x => fmt.fecha(x.fechaVencimiento) },
      { t: 'Total', cls: 'num', v: x => fmt.moneda(x.total) },
      { t: 'Saldo', cls: 'num', v: x => fmt.moneda(x.saldo) },
      { t: 'Estado', v: x => badgeEstado(x.estado) }
    ], ec.comprobantes, { vacio: 'Sin comprobantes.' }))}`;
  $$('[data-comp]', c).forEach(l => l.addEventListener('click', e => { e.preventDefault(); verComprobante(l.dataset.comp); }));
  $('#pagar', c)?.addEventListener('click', () => dialogoPagoVentanilla(a.id, a.apellido + ', ' + a.nombre, () => renderActual()));
}

async function tabPagos(c, a) {
  const pagos = await api.get(`/api/cobranza/pagos/afiliado/${a.id}`);
  c.innerHTML = tabla([
    { t: 'Fecha', v: p => fmt.fecha(p.fechaPago) },
    { t: 'Medio', v: p => esc(p.medio) },
    { t: 'Afiliado', v: p => esc(p.afiliado) },
    { t: 'Referencia', v: p => esc(p.referencia || ('#' + p.id)) },
    { t: 'Importe', cls: 'num', v: p => fmt.moneda(p.importe) },
    { t: 'Comprobante', v: p => p.tieneAdjunto ? `<a href="/api/cobranza/pagos/${p.id}/comprobante" target="_blank">${esc(p.adjuntoNombre)}</a>` : '<span class="suave">archivo del ente</span>' }
  ], pagos, { vacio: 'No hay pagos registrados.' });
}

async function tabHistoria(c, a) {
  const entradas = await api.get(`/api/afiliados/${a.id}/historia-clinica`, { grupo: true });
  c.innerHTML = html`
    <div class="cinta info">Información confidencial. Acceso restringido a auditoría médica y mesa de entradas; cada consulta queda registrada.</div>
    <div class="fila" style="justify-content:flex-end;margin-bottom:10px">${raw(a.estado === 'ACTIVO' ? '<button class="btn primario" id="nuevaHc">+ Nueva entrada</button>' : '')}</div>
    ${raw(tabla([
      { t: 'Fecha', v: x => fmt.fecha(x.fecha) },
      { t: 'Afiliado', v: x => esc(x.afiliado) },
      { t: 'Tipo', v: x => `<span class="badge info">${esc(x.tipo)}</span>` },
      { t: 'Profesional', v: x => esc(x.profesional) },
      { t: 'Diagnóstico / detalle', v: x => `<b>${esc(x.diagnostico || '')}</b><div>${esc(x.detalle)}</div>` },
      { t: 'Registrado', v: x => `<span class="suave">${esc(x.registradoPor)}<br>${fmt.fechaHora(x.registradoEl)}</span>` }
    ], entradas, { vacio: 'Sin entradas de historia clínica para el grupo familiar.' }))}`;
  $('#nuevaHc', c)?.addEventListener('click', () => {
    const m = modal('Nueva entrada de historia clínica', `
      <form class="form-grid">
        <div class="campo"><label>Fecha</label><input type="date" name="fecha" value="${fmt.hoy()}" required></div>
        <div class="campo"><label>Tipo</label><select name="tipo">${opciones(['CONSULTA', 'GUARDIA', 'INTERNACION', 'ESTUDIO', 'OTRO'].map(x => ({ id: x, nombre: x })), 'CONSULTA')}</select></div>
        <div class="campo"><label>Profesional</label><input name="profesional"></div>
        <div class="campo"><label>Diagnóstico</label><input name="diagnostico"></div>
        <div class="campo" style="grid-column:1/-1"><label>Detalle</label><textarea name="detalle" required></textarea></div>
      </form>
      <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn primario" id="ok">Guardar</button></div>`);
    $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
      try {
        await api.post(`/api/afiliados/${a.id}/historia-clinica`, datosForm($('form', m.el)));
        m.cerrar(); toast('Entrada registrada', 'ok'); renderActual();
      } catch (er) { error(er); }
    }));
  });
}

async function tabBloqueos(c, a) {
  const h = await api.get(`/api/afiliados/${a.id}/bloqueos`);
  c.innerHTML = tabla([
    { t: 'Fecha', v: x => fmt.fechaHora(x.fecha) },
    { t: 'Acción', v: x => badgeEstado(x.accion) },
    { t: 'Marca', v: x => esc(x.marca || '') },
    { t: 'Motivo', v: x => esc(x.motivo) },
    { t: 'Usuario', v: x => esc(x.usuario) }
  ], h, { vacio: 'El grupo nunca fue bloqueado.' });
}

async function tabAuditoria(c, a) {
  const h = await api.get(`/api/afiliados/${a.titularId || a.id}/auditoria`);
  c.innerHTML = tabla([
    { t: 'Fecha', v: x => fmt.fechaHora(x.fecha) },
    { t: 'Acción', v: x => `<span class="badge">${esc(x.accion)}</span>` },
    { t: 'Detalle', v: x => esc(x.detalle) },
    { t: 'Usuario', v: x => esc(x.usuario) }
  ], h, { vacio: 'Sin registros.' });
}

function renderActual() { window.dispatchEvent(new HashChangeEvent('hashchange')); }

// =====================================================================  DIÁLOGOS
async function formularioAfiliado(a) {
  try {
    const [planes, areas, derivadoras, medios] = await Promise.all([
      api.get('/api/cobertura/planes'), api.get('/api/areas'), api.get('/api/cobertura/derivadoras'), api.get('/api/cobranza/medios')]);
    const x = a || { carencia: 'COMUN', tipoDocumento: 'DNI', sexo: 'F', areaId: areas[0]?.id };
    const sectores = (areaId) => (areas.find(ar => String(ar.id) === String(areaId))?.sectores || []);
    const m = modal(a ? 'Editar afiliado' : 'Nuevo afiliado titular', `
      <form class="form-grid">
        <div class="campo"><label>Apellido *</label><input name="apellido" value="${esc(x.apellido)}" required></div>
        <div class="campo"><label>Nombre *</label><input name="nombre" value="${esc(x.nombre)}" required></div>
        <div class="campo"><label>Tipo documento *</label><select name="tipoDocumento">${opciones(['DNI', 'LC', 'LE', 'PAS', 'CI'].map(v => ({ id: v, nombre: v })), x.tipoDocumento)}</select></div>
        <div class="campo"><label>Documento *</label><input name="documento" value="${esc(x.documento)}" required></div>
        <div class="campo"><label>CUIL</label><input name="cuil" value="${esc(x.cuil)}" placeholder="27-12345678-9"></div>
        <div class="campo"><label>Fecha de nacimiento *</label><input type="date" name="fechaNacimiento" value="${esc(x.fechaNacimiento)}" required></div>
        <div class="campo"><label>Sexo *</label><select name="sexo">${opciones([{ id: 'F', nombre: 'Femenino' }, { id: 'M', nombre: 'Masculino' }, { id: 'X', nombre: 'X' }], x.sexo)}</select></div>
        <div class="campo"><label>Email</label><input type="email" name="email" value="${esc(x.email)}"></div>
        <div class="campo"><label>Teléfono</label><input name="telefono" value="${esc(x.telefono)}"></div>
        <div class="campo"><label>Domicilio</label><input name="domicilio" value="${esc(x.domicilio)}"></div>
        <div class="campo"><label>Localidad</label><input name="localidad" value="${esc(x.localidad)}"></div>
        <div class="campo"><label>Plan *</label><select name="planId" required>${opciones(planes.filter(p => p.activo), x.planId, { texto: p => `${p.codigo} ${p.nombre}`, vacio: 'Seleccionar…' })}</select></div>
        <div class="campo"><label>Área *</label><select name="areaId" id="selArea">${opciones(areas, x.areaId)}</select></div>
        <div class="campo"><label>Sector *</label><select name="sectorId" id="selSector">${opciones(sectores(x.areaId), x.sectorId)}</select></div>
        <div class="campo"><label>Obra social de origen</label><select name="derivadoraId">${opciones(derivadoras.filter(d => d.activa), x.derivadoraId, { vacio: 'Ninguna' })}</select></div>
        <div class="campo"><label>Vía de cobro</label><select name="viaCobro">${opciones(medios, x.viaCobro, { valor: 'codigo', vacio: '—' })}</select></div>
        <div class="campo"><label>Carencia *</label><select name="carencia">${opciones(['COMUN', 'REDUCIDA', 'SIN_CARENCIA'].map(v => ({ id: v, nombre: v })), x.carencia)}</select></div>
        <div class="campo"><label>Estado especial</label><input name="estadoEspecial" value="${esc(x.estadoEspecial)}" placeholder="Ej.: Discapacidad - auditar"></div>
        <div class="campo"><label>Vigente desde</label><input type="date" name="estadoEspecialDesde" value="${esc(x.estadoEspecialDesde)}"></div>
        <div class="campo"><label>Vigente hasta</label><input type="date" name="estadoEspecialHasta" value="${esc(x.estadoEspecialHasta)}"></div>
        ${a ? '' : `<div class="campo"><label>Fecha de alta</label><input type="date" name="fechaAlta" value="${fmt.hoy()}"></div>`}
      </form>
      <p class="ayuda">${a && !a.titularId ? 'Los cambios de plan, área, sector y vía de cobro se aplican a todo el grupo familiar.' : 'El número de afiliado se asigna automáticamente.'}</p>
      <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn primario" id="ok">Guardar</button></div>`);
    $('#selArea', m.el).addEventListener('change', e => { $('#selSector', m.el).innerHTML = opciones(sectores(e.target.value), null); });
    $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
      try {
        const d = datosForm($('form', m.el));
        if (a) {
          await api.put(`/api/afiliados/${a.id}`, d);
          m.cerrar(); toast('Afiliado actualizado', 'ok'); renderActual();
        } else {
          const r = await api.post('/api/afiliados', d);
          m.cerrar(); toast('Afiliado creado', 'ok'); location.hash = `#/afiliados/${r.id}`;
        }
      } catch (er) { error(er); }
    }));
  } catch (e) { error(e); }
}

function formularioIntegrante(titular) {
  const m = modal(`Agregar integrante al grupo de ${titular.apellido}`, `
    <form class="form-grid">
      <div class="campo"><label>Apellido *</label><input name="apellido" value="${esc(titular.apellido)}" required></div>
      <div class="campo"><label>Nombre *</label><input name="nombre" required></div>
      <div class="campo"><label>Parentesco *</label><select name="parentesco">${opciones(['CONYUGE', 'HIJO', 'OTRO'].map(v => ({ id: v, nombre: v })), 'HIJO')}</select></div>
      <div class="campo"><label>Tipo documento *</label><select name="tipoDocumento">${opciones(['DNI', 'PAS'].map(v => ({ id: v, nombre: v })), 'DNI')}</select></div>
      <div class="campo"><label>Documento *</label><input name="documento" required></div>
      <div class="campo"><label>Fecha de nacimiento *</label><input type="date" name="fechaNacimiento" required></div>
      <div class="campo"><label>Sexo *</label><select name="sexo">${opciones([{ id: 'F', nombre: 'Femenino' }, { id: 'M', nombre: 'Masculino' }, { id: 'X', nombre: 'X' }], 'F')}</select></div>
      <div class="campo"><label>Email</label><input type="email" name="email"></div>
      <div class="campo"><label>Estado especial</label><input name="estadoEspecial"></div>
    </form>
    <p class="ayuda">Hereda plan (${esc(titular.planCodigo)}), área, sector y vía de cobro del titular. Desde la próxima cuota se factura un integrante más.</p>
    <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn primario" id="ok">Agregar</button></div>`);
  $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
    try {
      await api.post(`/api/afiliados/${titular.id}/integrantes`, datosForm($('form', m.el)));
      m.cerrar(); toast('Integrante agregado', 'ok'); location.hash = `#/afiliados/${titular.id}/grupo`; renderActual();
    } catch (er) { error(er); }
  }));
}

function dialogoBloqueo(a, bloquear) {
  const m = modal(bloquear ? 'Bloquear grupo familiar' : 'Desbloquear grupo familiar', `
    <p class="ayuda">${bloquear ? 'El grupo sigue activo en la obra social, pero no podrá pedir órdenes ni atenderse hasta que se lo desbloquee.' : 'El grupo vuelve a quedar habilitado.'}</p>
    <form class="form-grid">
      ${bloquear ? '<div class="campo"><label>Marca</label><input name="marca" value="Bloqueado por falta de pago"></div>' : ''}
      <div class="campo" style="grid-column:1/-1"><label>Motivo *</label><textarea name="motivo" required></textarea></div>
    </form>
    <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn ${bloquear ? 'peligro' : 'primario'}" id="ok">${bloquear ? 'Bloquear' : 'Desbloquear'}</button></div>`);
  $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
    try {
      await api.post(`/api/afiliados/${a.id}/${bloquear ? 'bloqueo' : 'desbloqueo'}`, datosForm($('form', m.el)));
      m.cerrar(); toast(bloquear ? 'Grupo bloqueado' : 'Grupo desbloqueado', 'ok'); renderActual();
    } catch (er) { error(er); }
  }));
}

function dialogoBaja(a) {
  const m = modal('Dar de baja', `
    <p class="ayuda">${a.titularId ? 'Se da de baja solo a este integrante.' : '<b>Es el titular:</b> se da de baja a todo el grupo familiar.'}</p>
    <form class="form-grid">
      <div class="campo"><label>Fecha de baja *</label><input type="date" name="fechaBaja" value="${fmt.hoy()}" required></div>
      <div class="campo"><label>Motivo *</label><select name="motivo">${opciones(['Baja por renuncia', 'Fallecimiento', 'Cambio de obra social', 'Falta de pago', 'Otro'].map(v => ({ id: v, nombre: v })), 'Baja por renuncia')}</select></div>
    </form>
    <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn peligro" id="ok">Confirmar baja</button></div>`);
  $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
    try {
      const r = await api.post(`/api/afiliados/${a.id}/baja`, datosForm($('form', m.el)));
      m.cerrar(); toast(`${r.integrantesDadosDeBaja} integrante(s) dados de baja`, 'ok'); renderActual();
    } catch (er) { error(er); }
  }));
}

export async function verComprobante(id) {
  try {
    const c = await api.get(`/api/facturacion/comprobantes/${id}`);
    modal(`Comprobante ${c.numero}`, html`
      <div class="cinta aviso">Comprobante interno de cuota. No válido como factura.</div>
      <div class="datos" style="margin-bottom:12px">
        ${raw(dato('Afiliado', c.nroAfiliado + ' ' + c.apellidoNombre))}${raw(dato('Período', fmt.periodo(c.periodo)))}
        ${raw(dato('Emisión', fmt.fecha(c.fechaEmision)))}${raw(dato('Vencimiento', fmt.fecha(c.fechaVencimiento)))}
        <div><div class="dato-etiqueta">Estado</div><div class="dato-valor">${raw(badgeEstado(c.estado))}</div></div>
      </div>
      ${raw(tabla([
        { t: 'Código', v: i => `<span class="mono">${esc(i.codigo)}</span>` },
        { t: 'Descripción', v: i => esc(i.descripcion) },
        { t: 'Cant.', cls: 'num', v: i => i.cantidad },
        { t: 'Unitario', cls: 'num', v: i => fmt.moneda(i.precioUnitario) },
        { t: 'Importe', cls: 'num', v: i => fmt.moneda(i.importe) },
        { t: 'Cuenta', v: i => `<span class="mono suave">${esc(i.cuentaContable)}</span>` }
      ], c.items, { pie: [{}, { v: 'Total' }, {}, {}, { cls: 'num', v: fmt.moneda(c.total) }, {}] }))}
      <h3>Pagos imputados</h3>
      ${raw(tabla([{ t: 'Pago', v: i => '#' + i.pagoId }, { t: 'Fecha', v: i => fmt.fecha(i.fecha) }, { t: 'Importe', cls: 'num', v: i => fmt.moneda(i.importe) }],
        c.imputaciones, { vacio: 'Sin pagos imputados.' }))}
      <p class="derecha"><b>Saldo: ${fmt.moneda(c.saldo)}</b></p>
      <div class="acciones"><button class="btn" data-cerrar>Cerrar</button></div>`);
  } catch (e) { error(e); }
}

export function dialogoPagoVentanilla(afiliadoId, nombre, alTerminar) {
  const m = modal(`Pago en ventanilla · ${nombre}`, `
    <form class="form-grid">
      <div class="campo"><label>Fecha de pago *</label><input type="date" name="fecha" value="${fmt.hoy()}" max="${fmt.hoy()}" required></div>
      <div class="campo"><label>Importe *</label><input name="importe" data-tipo="num" placeholder="0,00" required></div>
      <div class="campo" style="grid-column:1/-1"><label>Comprobante (JPG, PNG o PDF) *</label><input type="file" name="comprobante" accept=".jpg,.jpeg,.png,.pdf" required></div>
      <div class="campo" style="grid-column:1/-1"><label>Observaciones</label><input name="observaciones"></div>
    </form>
    <p class="ayuda">El pago se imputa automáticamente a las cuotas más antiguas del grupo familiar.</p>
    <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn primario" id="ok">Registrar pago</button></div>`);
  $('#ok', m.el).addEventListener('click', e => conBoton(e.target, async () => {
    try {
      const f = $('form', m.el);
      const d = datosForm(f);
      const archivo = f.comprobante.files[0];
      if (!archivo) throw new Error('Adjunte el comprobante del pago.');
      const fd = new FormData();
      fd.append('comprobante', archivo);
      await api.multipart('/api/cobranza/pagos', fd, { afiliadoId, fecha: d.fecha, importe: d.importe, medio: 'CAJA', observaciones: d.observaciones });
      m.cerrar(); toast('Pago registrado e imputado', 'ok');
      alTerminar && alTerminar();
    } catch (er) { error(er); }
  }));
}
