// Enrutador de la aplicación (hash). Cada vista es un módulo con export function render(contenedor, parametros).
import { operador, guardarOperador } from './api.js';
import { error } from './ui.js';

const RUTAS = {
  inicio: { titulo: 'Inicio', modulo: './vistas/inicio.js' },
  afiliados: { titulo: 'Afiliados', modulo: './vistas/afiliados.js' },
  planes: { titulo: 'Planes y cobertura', modulo: './vistas/planes.js' },
  prestadores: { titulo: 'Prestadores y coseguro', modulo: './vistas/prestadores.js' },
  derivadoras: { titulo: 'Derivadoras', modulo: './vistas/derivadoras.js' },
  cuotas: { titulo: 'Cuotas', modulo: './vistas/cuotas.js' },
  cobranza: { titulo: 'Cobranza', modulo: './vistas/cobranza.js' },
  morosos: { titulo: 'Morosos', modulo: './vistas/morosos.js' },
  contabilidad: { titulo: 'Exportación a FEBBO', modulo: './vistas/contabilidad.js' },
  configuracion: { titulo: 'Configuración', modulo: './vistas/configuracion.js' }
};

async function navegar() {
  const hash = location.hash.replace(/^#\/?/, '') || 'inicio';
  const [nombre, ...resto] = hash.split('/');
  const ruta = RUTAS[nombre] || RUTAS.inicio;
  document.getElementById('titulo').textContent = ruta.titulo;
  document.title = `${ruta.titulo} · Sistema Integral CMM`;
  document.querySelectorAll('#menu a[data-ruta]').forEach(a => a.classList.toggle('activo', a.dataset.ruta === (RUTAS[nombre] ? nombre : 'inicio')));
  document.getElementById('sidebar').classList.remove('abierto');
  const cont = document.getElementById('vista');
  try {
    const mod = await import(ruta.modulo);
    await mod.render(cont, resto.map(decodeURIComponent));
  } catch (e) {
    error(e);
    cont.innerHTML = `<div class="tarjeta"><div class="cinta error">No se pudo cargar la pantalla: ${e.message}</div></div>`;
  }
  window.scrollTo(0, 0);
}

const inputOp = document.getElementById('operador');
inputOp.value = operador();
inputOp.addEventListener('change', () => guardarOperador(inputOp.value.trim()));
document.getElementById('btnMenu').addEventListener('click', () => document.getElementById('sidebar').classList.toggle('abierto'));
window.addEventListener('hashchange', navegar);
navegar();
