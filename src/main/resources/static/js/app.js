// Enrutador de la aplicación (hash). Cada vista es un módulo con export function render(contenedor, parametros).
import { api, consultarSesion, ingresar, salir, sesion, tieneRol } from './api.js';
import { ACCESO_PANTALLAS } from './permisos.js';
import { error, toast } from './ui.js';

const RUTAS = {
  inicio: { titulo: 'Inicio', modulo: './vistas/inicio.js' },
  afiliados: { titulo: 'Afiliados', modulo: './vistas/afiliados.js' },
  planes: { titulo: 'Planes y cobertura', modulo: './vistas/planes.js' },
  prestadores: { titulo: 'Prestadores y coseguro', modulo: './vistas/prestadores.js' },
  derivadoras: { titulo: 'Derivadoras', modulo: './vistas/derivadoras.js' },
  cuotas: { titulo: 'Cuotas', modulo: './vistas/cuotas.js' },
  cobranza: { titulo: 'Cobranza', modulo: './vistas/cobranza.js' },
  morosos: { titulo: 'Morosos', modulo: './vistas/morosos.js' },
  contabilidad: { titulo: 'Exportación a sistema contable', modulo: './vistas/contabilidad.js' },
  ordenes: { titulo: 'Órdenes', modulo: './vistas/ordenes.js' },
  auditoria: { titulo: 'Auditoría médica', modulo: './vistas/auditoria.js' },
  configuracion: { titulo: 'Configuración', modulo: './vistas/configuracion.js' }
};

function pantallaPermitida(nombre) {
  const roles = ACCESO_PANTALLAS[nombre];
  return !roles || tieneRol(roles);
}

async function navegar() {
  if (!sesion()) return;
  const hash = location.hash.replace(/^#\/?/, '') || 'inicio';
  const [nombre, ...resto] = hash.split('/');
  const ruta = RUTAS[nombre] || RUTAS.inicio;
  document.getElementById('titulo').textContent = ruta.titulo;
  document.title = `${ruta.titulo} · Sistema Integral CMM`;
  document.querySelectorAll('#menu a[data-ruta]').forEach(a => a.classList.toggle('activo', a.dataset.ruta === (RUTAS[nombre] ? nombre : 'inicio')));
  document.getElementById('sidebar').classList.remove('abierto');
  document.querySelectorAll('.modal-fondo').forEach(m => m.remove()); // un diálogo abierto no queda sobre otra pantalla
  const cont = document.getElementById('vista');
  if (RUTAS[nombre] && !pantallaPermitida(nombre)) {
    cont.innerHTML = '<div class="tarjeta"><div class="cinta aviso">Su usuario no tiene acceso a esta pantalla.</div></div>';
    return;
  }
  try {
    const mod = await import(ruta.modulo);
    await mod.render(cont, resto.map(decodeURIComponent));
  } catch (e) {
    error(e);
    cont.innerHTML = `<div class="tarjeta"><div class="cinta error">No se pudo cargar la pantalla: ${e.message}</div></div>`;
  }
  window.scrollTo(0, 0);
}

// ---------- Aviso a Mesa de Entradas de las órdenes que resolvió la auditoría (T30)
// Cada minuto pregunta qué órdenes se resolvieron desde el último aviso; muestra un mensaje y un contador en el menú.
const ROLES_AVISO = ['MESA_ENTRADAS', 'ADMINISTRACION'];
const AVISOS_CADA_MS = 60000;
let avisoTimer = null;
let avisoDesde = null;
let avisosSinVer = 0;
const ahoraLocal = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().substring(0, 19);

function marcarMenuOrdenes() {
  const a = document.querySelector('#menu a[data-ruta="ordenes"]');
  if (!a) return;
  a.querySelector('.badge')?.remove();
  if (avisosSinVer > 0) a.insertAdjacentHTML('beforeend', ` <span class="badge error" title="Órdenes resueltas por auditoría">${avisosSinVer}</span>`);
}

async function consultarAvisos() {
  if (!sesion() || !tieneRol(ROLES_AVISO)) return;
  try {
    const avisos = await api.get('/api/ordenes/avisos', { desde: avisoDesde });
    if (!avisos.length) return;
    avisoDesde = avisos[avisos.length - 1].fechaResolucion; // hora del servidor: no depende del reloj de la PC
    avisos.forEach(a => toast(`Orden ${a.numero} (${a.afiliado}): ${a.estado === 'AUTORIZADA' ? 'autorizada' : 'rechazada'} por auditoría`,
      a.estado === 'AUTORIZADA' ? 'ok' : ''));
    avisosSinVer += avisos.length;
    marcarMenuOrdenes();
  } catch (e) { /* sin conexión: se reintenta en el próximo minuto */ }
}

function iniciarAvisos() {
  detenerAvisos();
  if (!tieneRol(ROLES_AVISO)) return;
  avisoDesde = ahoraLocal();
  avisoTimer = setInterval(consultarAvisos, AVISOS_CADA_MS);
}

function detenerAvisos() {
  clearInterval(avisoTimer);
  avisoTimer = null;
  avisosSinVer = 0;
  marcarMenuOrdenes();
}

window.addEventListener('cmm:avisos-vistos', () => { avisosSinVer = 0; marcarMenuOrdenes(); });

// ---------- Sesión (T36)
const $login = document.getElementById('login');
const $layout = document.getElementById('layout');
const $loginError = document.getElementById('loginError');

// Ojito de la contraseña: alterna entre ocultarla y mostrarla.
function verClave(ver) {
  document.getElementById('loginClave').type = ver ? 'text' : 'password';
  const boton = document.getElementById('btnVerClave');
  boton.setAttribute('aria-pressed', String(ver));
  boton.setAttribute('aria-label', ver ? 'Ocultar contraseña' : 'Mostrar contraseña');
  boton.title = ver ? 'Ocultar contraseña' : 'Mostrar contraseña';
  boton.querySelector('.ojo-abierto').hidden = ver;
  boton.querySelector('.ojo-cerrado').hidden = !ver;
}
document.getElementById('btnVerClave').addEventListener('click', () => {
  verClave(document.getElementById('loginClave').type === 'password');
});

function mostrarLogin(mensaje) {
  detenerAvisos();
  $layout.hidden = true;
  $login.hidden = false;
  $loginError.hidden = !mensaje;
  $loginError.textContent = mensaje || '';
  document.getElementById('formLogin').reset();
  verClave(false);
  document.getElementById('loginUsuario').focus();
}

function mostrarApp() {
  const s = sesion();
  $login.hidden = true;
  $layout.hidden = false;
  document.getElementById('sesionNombre').textContent = s.nombre;
  // Menú: solo las pantallas permitidas; un título de grupo sin opciones visibles también se oculta.
  document.querySelectorAll('#menu a[data-ruta]').forEach(a => { a.hidden = !pantallaPermitida(a.dataset.ruta); });
  document.querySelectorAll('#menu .menu-grupo').forEach(g => {
    let sig = g.nextElementSibling, visible = false;
    while (sig && !sig.classList.contains('menu-grupo')) { if (!sig.hidden) visible = true; sig = sig.nextElementSibling; }
    g.hidden = !visible;
  });
  iniciarAvisos();
  navegar();
}

document.getElementById('formLogin').addEventListener('submit', async ev => {
  ev.preventDefault();
  const boton = document.getElementById('btnIngresar');
  boton.disabled = true;
  try {
    await ingresar(document.getElementById('loginUsuario').value.trim(), document.getElementById('loginClave').value);
    location.hash = '#/inicio';
    mostrarApp();
  } catch (e) {
    mostrarLogin(e.message);
  } finally {
    boton.disabled = false;
  }
});

document.getElementById('btnSalir').addEventListener('click', async () => {
  try { await salir(); } catch (e) { /* si falla igual se vuelve al ingreso */ }
  mostrarLogin();
});

window.addEventListener('cmm:sesion-vencida', () => mostrarLogin('Su sesión venció. Ingrese nuevamente.'));
document.getElementById('btnMenu').addEventListener('click', () => document.getElementById('sidebar').classList.toggle('abierto'));
window.addEventListener('hashchange', navegar);

(async () => {
  try {
    if (await consultarSesion()) mostrarApp(); else mostrarLogin();
  } catch (e) {
    mostrarLogin('No se pudo conectar con el servidor: ' + e.message);
  }
})();
