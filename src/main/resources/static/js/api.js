// Cliente de la API REST. Todas las pantallas leen y escriben datos reales a través de este módulo.

const CLAVE_OPERADOR = 'cmm.operador';

export function operador() {
  try { return localStorage.getItem(CLAVE_OPERADOR) || ''; } catch { return ''; }
}

export function guardarOperador(nombre) {
  try { localStorage.setItem(CLAVE_OPERADOR, nombre); } catch { /* sin almacenamiento */ }
}

function encabezados(extra = {}) {
  const h = { ...extra };
  const op = operador();
  if (op) h['X-Usuario'] = encodeURIComponent(op); // identifica al operador en la auditoría
  return h;
}

async function procesar(resp) {
  if (resp.status === 204) return null;
  const tipo = resp.headers.get('content-type') || '';
  const cuerpo = tipo.includes('json') ? await resp.json() : await resp.text();
  if (!resp.ok) {
    const msg = (cuerpo && (cuerpo.detail || cuerpo.title)) || `Error ${resp.status}`;
    const err = new Error(msg);
    err.status = resp.status;
    throw err;
  }
  return cuerpo;
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
