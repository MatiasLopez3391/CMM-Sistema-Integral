import { api, fmt } from '../api.js';
import { html, raw, esc, $, $$, tabla, modal, datosForm, toast, error, badgeEstado, badgeBloqueado, cargando, conBoton, botonesDescarga, enlazarDescarga } from '../ui.js';

export async function render(el, params) {
  cargando(el);
  const alDia = params[0] || fmt.hoy();
  const [morosos, notif, mora] = await Promise.all([
    api.get('/api/facturacion/morosos', { alDia }),
    api.get('/api/facturacion/notificaciones'),
    api.get('/api/facturacion/mora', { fecha: alDia })
  ]);
  const total = morosos.reduce((s, m) => s + Number(m.deudaVencida), 0);
  el.innerHTML = html`
    <div class="tarjeta">
      <div class="tarjeta-titulo">
        <h2>Proceso de mora del ${fmt.fecha(alDia)}</h2>
        <div class="campo chico"><label>Al día</label><input type="date" id="alDiaMora" value="${alDia}"></div>
      </div>
      <div class="cinta info">Desde el día ${mora.diaBloqueo} se deja de dar órdenes a quien no pagó la cuota: el sistema propone el bloqueo y un usuario lo confirma.
      Con ${mora.cuotasBaja} o más cuotas impagas el grupo figura para dar de baja; <b>la baja se hace siempre a mano</b>, desde la ficha del afiliado.</div>
      ${raw(mora.antesDelDiaDeBloqueo ? `<div class="cinta aviso">Todavía no es día ${mora.diaBloqueo}: la lista de bloqueos es informativa.</div>` : '')}
      <div class="tarjeta-titulo" style="margin-top:12px"><h3>Para bloquear (${mora.paraBloquear.length})</h3>
        <div class="fila"><label style="display:inline-flex;gap:6px"><input type="checkbox" id="todosBloq"> Seleccionar todos</label>
        <button class="btn peligro" id="bloquearSel" ${raw(mora.paraBloquear.length ? '' : 'disabled')}>Bloquear seleccionados</button></div></div>
      ${raw(tabla([
        { t: '', v: m => `<input type="checkbox" class="selBloq" value="${m.afiliadoId}">` },
        { t: 'Nro. afiliado', v: m => `<span class="mono">${esc(m.nroAfiliado)}</span>` },
        { t: 'Titular', v: m => `<a href="#/afiliados/${m.afiliadoId}/cuenta">${esc(m.apellidoNombre)}</a>` },
        { t: 'Cuotas vencidas', cls: 'num', v: m => m.cuotasVencidas },
        { t: 'Debe', cls: 'num', v: m => `<b>${fmt.moneda(m.deudaVencida)}</b>` },
        { t: 'Desde', v: m => `${fmt.fecha(m.vencimientoMasAntiguo)} <span class="suave">(${m.diasAtraso} días)</span>` }
      ], mora.paraBloquear, { vacio: 'No hay grupos para bloquear.' }))}
      <h3>Para dar de baja: ${mora.cuotasBaja} o más cuotas impagas (${mora.paraBaja.length})</h3>
      ${raw(tabla([
        { t: 'Nro. afiliado', v: m => `<span class="mono">${esc(m.nroAfiliado)}</span>` },
        { t: 'Titular', v: m => `${esc(m.apellidoNombre)} ${badgeBloqueado(m.bloqueado)}` },
        { t: 'Cuotas vencidas', cls: 'num', v: m => m.cuotasVencidas },
        { t: 'Debe', cls: 'num', v: m => `<b>${fmt.moneda(m.deudaVencida)}</b>` },
        { t: 'Desde', v: m => `${fmt.fecha(m.vencimientoMasAntiguo)} <span class="suave">(${m.diasAtraso} días)</span>` },
        { t: '', cls: 'derecha', v: m => `<a class="btn chico" href="#/afiliados/${m.afiliadoId}/datos">Abrir ficha para dar de baja</a>` }
      ], mora.paraBaja, { vacio: 'Ningún grupo llega a ese número de cuotas impagas.' }))}
    </div>
    <div class="tarjeta">
      <div class="tarjeta-titulo">
        <h2>Afiliados con cuotas vencidas</h2>
        <div class="fila">
          <div class="campo chico"><label>Al día</label><input type="date" id="alDia" value="${alDia}"></div>
          ${raw(botonesDescarga('mor'))}
          <button class="btn" id="textoAviso">Texto del aviso</button>
          <button class="btn" id="avisar">Enviar aviso por mail a seleccionados</button>
        </div>
      </div>
      <label style="display:inline-flex;gap:6px;margin-bottom:8px"><input type="checkbox" id="todosMail"> Seleccionar a todos los que tienen email (${morosos.filter(m => m.email).length})</label>
      <div class="cinta info">Un grupo bloqueado sigue activo en la obra social, pero no puede pedir órdenes ni atenderse hasta que se lo desbloquee.</div>
      <div class="grid-kpi">
        <div class="kpi alerta"><div class="kpi-etiqueta">Deuda vencida total</div><div class="kpi-valor">${fmt.moneda(total)}</div></div>
        <div class="kpi"><div class="kpi-etiqueta">Grupos morosos</div><div class="kpi-valor">${morosos.length}</div></div>
        <div class="kpi"><div class="kpi-etiqueta">Ya bloqueados</div><div class="kpi-valor">${morosos.filter(m => m.bloqueado).length}</div></div>
      </div>
      ${raw(tabla([
        { t: '', v: m => `<input type="checkbox" class="sel" value="${m.afiliadoId}" ${m.email ? '' : 'disabled title="Sin email"'}>` },
        { t: 'Nro. afiliado', v: m => `<span class="mono">${esc(m.nroAfiliado)}</span>` },
        { t: 'Titular', v: m => `<a href="#/afiliados/${m.afiliadoId}/cuenta">${esc(m.apellidoNombre)}</a> ${badgeBloqueado(m.bloqueado)}` },
        { t: 'Plan', v: m => esc(m.planCodigo) },
        { t: 'Cuotas vencidas', cls: 'num', v: m => m.cuotasVencidas },
        { t: 'Deuda vencida', cls: 'num', v: m => `<b>${fmt.moneda(m.deudaVencida)}</b>` },
        { t: 'Vencimiento más antiguo', v: m => `${fmt.fecha(m.vencimientoMasAntiguo)} <span class="suave">(${m.diasAtraso} días)</span>` },
        { t: 'Email', v: m => m.email ? esc(m.email) : '<span class="badge aviso">Sin email</span>' },
        { t: '', cls: 'derecha', v: m => m.bloqueado ? '' : `<button class="btn chico peligro" data-bloq="${m.afiliadoId}">Bloquear</button>` }
      ], morosos, { vacio: 'No hay afiliados con cuotas vencidas a esa fecha.' }))}
    </div>
    <div class="tarjeta">
      <h2>Avisos enviados</h2>
      ${raw(notif.smtpConfigurado ? '' : '<div class="cinta aviso">El servidor de correo no está configurado: los avisos quedan registrados sin enviar y se pueden consultar en esta lista.</div>')}
      ${raw(tabla([
        { t: 'Fecha', v: n => fmt.fechaHora(n.fechaCreacion) },
        { t: 'Destinatario', v: n => esc(n.destinatario || '—') },
        { t: 'Asunto', v: n => esc(n.asunto) },
        { t: 'Estado', v: n => badgeEstado(n.estado) },
        { t: '', v: (n) => `<a href="#" data-ver="${n.id}">Ver mensaje</a>` }
      ], notif.notificaciones.slice(0, 30), { vacio: 'Todavía no se enviaron avisos.' }))}
    </div>`;

  enlazarDescarga(el, 'mor', '/api/facturacion/imprimir/morosos', () => ({ alDia }), api);
  $('#alDia', el).addEventListener('change', e => { location.hash = `#/morosos/${e.target.value}`; });
  $('#alDiaMora', el).addEventListener('change', e => { location.hash = `#/morosos/${e.target.value}`; });
  $('#todosMail', el).addEventListener('change', e => $$('.sel', el).forEach(x => { if (!x.disabled) x.checked = e.target.checked; }));
  $('#todosBloq', el).addEventListener('change', e => $$('.selBloq', el).forEach(x => { x.checked = e.target.checked; }));
  $('#bloquearSel', el).addEventListener('click', e => conBoton(e.target, async () => {
    const ids = $$('.selBloq', el).filter(x => x.checked).map(x => Number(x.value));
    if (!ids.length) { toast('Seleccione al menos un grupo', 'error'); return; }
    if (!confirm(`¿Bloquear ${ids.length} grupo(s)? No podrán pedir órdenes hasta que se los desbloquee.`)) return;
    try {
      const r = await api.post('/api/facturacion/mora/bloquear', { afiliadoIds: ids, fecha: alDia });
      toast(`${r.bloqueados} grupo(s) bloqueados${r.omitidos.length ? ' · ' + r.omitidos.join('; ') : ''}`, 'ok');
      render(el, params);
    } catch (er) { error(er); }
  }));
  $('#textoAviso', el).addEventListener('click', async () => {
    try {
      const t = await api.get('/api/facturacion/mora/texto-aviso');
      const m = modal('Texto del aviso de deuda', `
        <div class="campo"><label>Asunto</label><input id="asunto" maxlength="200" value="${esc(t.asunto)}"></div>
        <div class="campo"><label>Texto</label><textarea id="texto" rows="11" maxlength="1800">${esc(t.texto)}</textarea></div>
        <p class="ayuda">Se reemplazan solos: <span class="mono">{nombre}</span>, <span class="mono">{fecha}</span>, <span class="mono">{cuotas}</span>,
        <span class="mono">{deuda}</span> y <span class="mono">{vencimiento}</span> (deuda más antigua).</p>
        <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn primario" id="ok">Guardar</button></div>`);
      $('#ok', m.el).addEventListener('click', ev => conBoton(ev.target, async () => {
        try {
          await api.put('/api/facturacion/mora/texto-aviso', { asunto: $('#asunto', m.el).value, texto: $('#texto', m.el).value });
          m.cerrar(); toast('Texto del aviso guardado', 'ok');
        } catch (er) { error(er); }
      }));
    } catch (er) { error(er); }
  });
  $('#avisar', el).addEventListener('click', e => conBoton(e.target, async () => {
    const ids = $$('.sel', el).filter(x => x.checked).map(x => Number(x.value));
    if (!ids.length) { toast('Seleccione al menos un afiliado con email', 'error'); return; }
    try {
      const r = await api.post('/api/facturacion/morosos/avisos', { afiliadoIds: ids });
      toast(`Enviados: ${r.enviados} · Registrados sin SMTP: ${r.registradosSinSmtp} · Sin email: ${r.sinEmail} · Errores: ${r.errores}`, 'ok');
      render(el, params);
    } catch (er) { error(er); }
  }));
  $$('[data-bloq]', el).forEach(b => b.addEventListener('click', () => {
    const m = morosos.find(x => String(x.afiliadoId) === b.dataset.bloq);
    const d = modal(`Bloquear a ${m.apellidoNombre}`, `
      <form class="form-grid">
        <div class="campo"><label>Marca</label><input name="marca" value="Bloqueado por falta de pago"></div>
        <div class="campo" style="grid-column:1/-1"><label>Motivo *</label><textarea name="motivo">${m.cuotasVencidas} cuota(s) vencida(s) por ${fmt.moneda(m.deudaVencida)}</textarea></div>
      </form>
      <div class="acciones"><button class="btn" data-cerrar>Cancelar</button><button class="btn peligro" id="ok">Bloquear</button></div>`);
    $('#ok', d.el).addEventListener('click', ev => conBoton(ev.target, async () => {
      try {
        await api.post(`/api/afiliados/${m.afiliadoId}/bloqueo`, datosForm($('form', d.el)));
        d.cerrar(); toast('Grupo bloqueado', 'ok'); render(el, params);
      } catch (er) { error(er); }
    }));
  }));
  $$('[data-ver]', el).forEach(a => a.addEventListener('click', ev => {
    ev.preventDefault();
    const n = notif.notificaciones.find(x => String(x.id) === a.dataset.ver);
    modal(n.asunto, `<p class="suave">Para: ${esc(n.destinatario || '—')} · ${badgeEstado(n.estado)}</p>
      <pre style="white-space:pre-wrap;font-family:inherit;background:#f7f9fc;padding:12px;border-radius:6px">${esc(n.cuerpo)}</pre>
      ${n.error ? `<div class="cinta error">${esc(n.error)}</div>` : ''}
      <div class="acciones"><button class="btn" data-cerrar>Cerrar</button></div>`);
  }));
}
