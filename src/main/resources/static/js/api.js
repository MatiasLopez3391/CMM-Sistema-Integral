// Cliente de la API REST. Todas las pantallas leen y escriben datos reales a través de este módulo.

// ---------- Sesión (T36): el usuario ingresa con su cuenta; el servidor guarda la sesión en una cookie HttpOnly.
let sesionActual = null;

/** Datos de quien tiene la sesión abierta: { usuario, nombre, roles } o null. */
export const sesion = () => sesionActual;

/** true si el usuario actual tiene alguno de esos roles. El servidor vuelve a controlarlo en cada pedido. */
export function tieneRol(roles) {
  return !!sesionActual && roles.some(r => sesionActual.roles.includes(r));
}

// Protección CSRF: el servidor deja la cookie XSRF-TOKEN y el navegador la devuelve en este encabezado.
function xsrf() {
  const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  return m ? decodeURIComponent(m[1]) : '';
}

function encabezados(extra = {}) {
  const h = { ...extra };
  const t = xsrf();
  if (t) h['X-XSRF-TOKEN'] = t;
  return h;
}

async function procesar(resp, avisarSesionVencida = true) {
  if (resp.status === 204) return null;
  const tipo = resp.headers.get('content-type') || '';
  const cuerpo = tipo.includes('json') ? await resp.json() : await resp.text();
  if (!resp.ok) {
    let msg = (cuerpo && (cuerpo.detail || cuerpo.title)) || `Error ${resp.status}`;
    if (resp.status === 401 && avisarSesionVencida) {
      sesionActual = null;
      msg = 'Su sesión venció. Ingrese nuevamente.';
      window.dispatchEvent(new Event('cmm:sesion-vencida'));
    }
    const err = new Error(msg);
    err.status = resp.status;
    throw err;
  }
  return cuerpo;
}

/** Consulta quién tiene la sesión abierta. Devuelve null si nadie (y deja lista la cookie CSRF para ingresar). */
export async function consultarSesion() {
  const resp = await fetch('/api/auth/yo', { headers: encabezados() });
  if (resp.status === 401) { sesionActual = null; return null; }
  sesionActual = await procesar(resp);
  return sesionActual;
}

export async function ingresar(usuario, clave) {
  const resp = await fetch('/api/auth/login', {
    method: 'POST',
    headers: encabezados({ 'Content-Type': 'application/x-www-form-urlencoded' }),
    body: new URLSearchParams({ usuario, clave })
  });
  await procesar(resp, false);
  return consultarSesion();
}

export async function salir() {
  await fetch('/api/auth/logout', { method: 'POST', headers: encabezados() });
  sesionActual = null;
  await consultarSesion(); // renueva la cookie CSRF para el próximo ingreso
}

function conQuery(url, params) {
  if (!params) return url;
  const q = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') q.append(k, v); });
  const s = q.toString();
  return s ? `${url}?${s}` : url;
}

export const api = {
  get: (url, params) => fetch(conQuery(url, params), { headers: encabezados() }).then(procesar),
  post: (url, body) => fetch(url, {
    method: 'POST', headers: encabezados({ 'Content-Type': 'application/json' }), body: JSON.stringify(body ?? {})
  }).then(procesar),
  put: (url, body) => fetch(url, {
    method: 'PUT', headers: encabezados({ 'Content-Type': 'application/json' }), body: JSON.stringify(body ?? {})
  }).then(procesar),
  multipart: (url, formData, params) => fetch(conQuery(url, params), {
    method: 'POST', headers: encabezados(), body: formData
  }).then(procesar),
  descargar: async (url, params) => {
    const resp = await fetch(conQuery(url, params), { headers: encabezados() });
    if (!resp.ok) await procesar(resp);
    const blob = await resp.blob();
    const disp = resp.headers.get('content-disposition') || '';
    const m = /filename="?([^";]+)"?/.exec(disp);
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = m ? m[1] : 'descarga';
    document.body.appendChild(a);
    a.click();
    setTimeout(() => { URL.revokeObjectURL(a.href); a.remove(); }, 500);
    return resp.headers;
  }
};

// ---------- Formatos (es-AR)
const fmtMoneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', minimumFractionDigits: 2 });
const fmtNumero = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 2 });

export const fmt = {
  moneda: v => (v === null || v === undefined || v === '') ? '—' : fmtMoneda.format(Number(v)),
  numero: v => (v === null || v === undefined) ? '—' : fmtNumero.format(Number(v)),
  pct: v => (v === null || v === undefined) ? '—' : `${fmtNumero.format(Number(v))} %`,
  fecha: v => {
    if (!v) return '—';
    const [a, m, d] = String(v).substring(0, 10).split('-');
    return `${d}/${m}/${a}`;
  },
  fechaHora: v => {
    if (!v) return '—';
    const d = new Date(v);
    return isNaN(d) ? v : d.toLocaleString('es-AR', { dateStyle: 'short', timeStyle: 'short' });
  },
  periodo: p => p ? `${p.substring(4, 6)}/${p.substring(0, 4)}` : '—',
  hoy: () => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().substring(0, 10)
};
