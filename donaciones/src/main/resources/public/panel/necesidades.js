import { apiUrl, request } from './api.js';
import { card, showAlert } from './ui.js';

const container = document.querySelector('#necesidades-lista');

async function cargarNecesidades(entidadId) {
  if (!entidadId) return;
  try {
    const necesidades = await request(apiUrl(`/beneficiarias/${encodeURIComponent(entidadId)}/necesidades`));
    container.innerHTML = necesidades.length ? necesidades.map((n) => card(n.id, `Tipo: ${n.tipo}<br>${n.descripcion}<br>Objetivo: ${n.cantidadObjetivo} ${n.unidad}`, `<button class="btn btn-sm btn-outline-danger" data-delete-need="${n.id}">Eliminar</button>`)).join('') : '<p class="text-body-secondary">No hay necesidades cargadas.</p>';
    container.querySelectorAll('[data-delete-need]').forEach((button) => button.addEventListener('click', async () => {
      try { await request(apiUrl(`/beneficiarias/${encodeURIComponent(entidadId)}/necesidades/${button.dataset.deleteNeed}`), { method: 'DELETE' }); await cargarNecesidades(entidadId); }
      catch (error) { showAlert(error.message); }
    }));
  } catch (error) { showAlert(error.message); }
}

export function registrarFormularioNecesidad() {
  document.querySelector('#form-necesidad')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const f = new FormData(e.target);
    const entidadId = f.get('entidadId');
    try {
      await request(apiUrl(`/beneficiarias/${encodeURIComponent(entidadId)}/necesidades`), { method: 'POST', body: JSON.stringify({ tipo: f.get('tipo'), subcategoria: f.get('subcategoria'), categoria: f.get('categoria'), cantidadObjetivo: Number(f.get('cantidadObjetivo')), unidad: f.get('unidad'), descripcion: f.get('descripcion') }) });
      await cargarNecesidades(entidadId);
    } catch (error) { showAlert(error.message); }
  });
}
