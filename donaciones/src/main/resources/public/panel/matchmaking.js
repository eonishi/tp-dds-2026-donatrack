import { apiUrl, request } from './api.js';
import { card, showAlert } from './ui.js';

const container = document.querySelector('#matchmaking-lista');

export async function cargarMatchmaking() {
  try {
    const propuestas = await request(apiUrl('/propuestas-asignacion'));
    container.innerHTML = propuestas.length ? propuestas.map(crearTarjeta).join('') : '<p class="text-body-secondary">No hay propuestas pendientes.</p>';
    registrarEventos();
  } catch (error) { showAlert(error.message); }
}

function crearTarjeta(propuesta) {
  const mejor = propuesta.rankings?.[0]?.candidatos?.[0];
  const boton = mejor ? `<button class="btn btn-sm btn-primary" data-select-match="${propuesta.donacionId}" data-entity-id="${mejor.entidadBeneficiariaId}">Seleccionar mejor candidato</button>` : '';
  const rankings = (propuesta.rankings || []).map((r) => `<strong>${r.algoritmo}</strong>: ${(r.candidatos || []).map((c) => `${c.razonSocial} (${c.puntaje})`).join(', ')}`).join('<br>');
  return card(propuesta.donacionId, `Pendiente: ${propuesta.pendiente}<br>${rankings}`, boton);
}

function registrarEventos() {
  container.querySelectorAll('[data-select-match]').forEach((button) => button.addEventListener('click', async () => {
    try {
      await request(apiUrl(`/donaciones/${button.dataset.selectMatch}/propuesta-asignacion`), { method: 'PUT', body: JSON.stringify({ entidadBeneficiariaId: Number(button.dataset.entityId) }) });
      await cargarMatchmaking();
    } catch (error) { showAlert(error.message); }
  }));
}

export function registrarFormularioMatchmaking() {
  document.querySelector('#form-matchmaking')?.addEventListener('submit', async (e) => {
    e.preventDefault(); const id = new FormData(e.target).get('donacionId');
    try { await request(apiUrl(`/donaciones/${encodeURIComponent(id)}/propuesta-asignacion`), { method: 'POST' }); await cargarMatchmaking(); }
    catch (error) { showAlert(error.message); }
  });
}
