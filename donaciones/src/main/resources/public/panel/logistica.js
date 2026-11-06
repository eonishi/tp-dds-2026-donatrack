import { logistica } from './api.js';
import { card, showAlert } from './ui.js';

export async function cargarLogistica() {
  try {
    const { camiones, rutas } = await logistica('{ camiones { patente volumenM3 capacidadCargaKg } rutas { id enCurso camion { patente } } }');
    mostrarCamiones(camiones); mostrarRutas(rutas); registrarEventosDeRutas();
  } catch (error) { showAlert(`No se pudo consultar logística: ${error.message}`); }
}

function mostrarCamiones(camiones) {
  const container = document.querySelector('#camiones-lista');
  container.innerHTML = camiones.length ? camiones.map((c) => `<p class="mb-2"><strong>${c.patente}</strong><br><span class="small text-body-secondary">${c.capacidadCargaKg} kg · ${c.volumenM3} m³</span></p>`).join('') : '<p class="text-body-secondary">No hay camiones.</p>';
}

function mostrarRutas(rutas) {
  const container = document.querySelector('#rutas-lista');
  container.innerHTML = rutas.length ? rutas.map((r) => card(r.id, `Camión: ${r.camion?.patente || 'sin patente'}<br>Estado: ${r.enCurso ? 'en curso' : 'pendiente'}`, `<button class="btn btn-sm btn-primary" data-route-start="${r.id}">Iniciar</button><button class="btn btn-sm btn-outline-secondary" data-route-finish="${r.id}">Finalizar</button>`)).join('') : '<p class="text-body-secondary">No hay rutas.</p>';
}

function registrarEventosDeRutas() {
  document.querySelectorAll('[data-route-start], [data-route-finish]').forEach((button) => button.addEventListener('click', async () => {
    const rutaId = button.dataset.routeStart || button.dataset.routeFinish;
    const mutacion = button.dataset.routeStart ? 'iniciarRuta' : 'finalizarRuta';
    try { await logistica(`mutation($id: ID!) { ${mutacion}(id: $id) { id } }`, { id: rutaId }); await cargarLogistica(); }
    catch (error) { showAlert(error.message); }
  }));
}

export function registrarFormularioCamion() {
  document.querySelector('#form-camion')?.addEventListener('submit', async (e) => {
    e.preventDefault(); const f = new FormData(e.target);
    try {
      await logistica('mutation($input: CamionInput!) { crearCamion(input: $input) { patente } }', { input: { patente: f.get('patente'), volumenM3: Number(f.get('volumenM3')), alturaM: Number(f.get('alturaM')), capacidadCargaKg: Number(f.get('capacidadCargaKg')) } });
      e.target.reset(); await cargarLogistica();
    } catch (error) { showAlert(error.message); }
  });
}
