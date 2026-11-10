import { apiUrl, request } from './api.js';
import { card, showAlert } from './ui.js';

const container = document.querySelector('#catalogo-lista');

export async function cargarCatalogo() {
  try {
    const subcategorias = await request(apiUrl('/subcategorias'));
    container.innerHTML = subcategorias.length ? subcategorias.map((s) => card(s.nombre, `Categoría: ${s.categoria}`)).join('') : '<p class="text-body-secondary">No hay subcategorías.</p>';
  } catch (error) { showAlert(error.message); }
}

export function registrarFormularioSubcategoria() {
  document.querySelector('#form-subcategoria')?.addEventListener('submit', async (e) => {
    e.preventDefault(); const f = new FormData(e.target);
    try {
      await request(apiUrl('/subcategorias'), { method: 'POST', body: JSON.stringify({ nombre: f.get('nombre'), categoria: f.get('categoria') }) });
      e.target.reset(); await cargarCatalogo();
    } catch (error) { showAlert(error.message); }
  });
}
