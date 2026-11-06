import { cargarDonaciones, registrarFormularioDonacion } from './panel/donaciones.js';
import { registrarFormularioNecesidad } from './panel/necesidades.js';
import { cargarLogistica, registrarFormularioCamion } from './panel/logistica.js';
import { cargarCatalogo, registrarFormularioSubcategoria } from './panel/catalogo.js';
import { cargarMatchmaking, registrarFormularioMatchmaking } from './panel/matchmaking.js';
import { apiUrl, request } from './panel/api.js';
import { cargarNecesidades } from './panel/necesidades.js';
import { showAlert } from './panel/ui.js';

const root = document.querySelector('.panel-app');

if (root) {
  prepararVista(root.dataset.seccion);
  registrarAccionesComunes();
  cargarOpciones(root.dataset.seccion);
  cargarPagina(root.dataset.seccion);
}

function prepararVista(seccion) {
  document.querySelectorAll('.panel-vista').forEach((vista) => {
    vista.classList.add('d-none');
  });

  document.querySelector(`#vista-${seccion}`)?.classList.remove('d-none');
}

function registrarAccionesComunes() {
  document.querySelector('[data-action="mostrar-form-donacion"]')?.addEventListener('click', () => {
    document.querySelector('#form-donacion').classList.remove('d-none');
  });

  document.querySelector('[data-action="ocultar-form-donacion"]')?.addEventListener('click', () => {
    document.querySelector('#form-donacion').classList.add('d-none');
  });

  document.querySelector('[data-action="recargar-logistica"]')?.addEventListener('click', cargarLogistica);
  document.querySelector('[data-action="recargar-matchmaking"]')?.addEventListener('click', cargarMatchmaking);

  registrarFormularioDonacion();
  registrarFormularioNecesidad();
  registrarFormularioCamion();
  registrarFormularioSubcategoria();
  registrarFormularioMatchmaking();
}

async function cargarOpciones(seccion) {
  if (seccion === 'donaciones') {
    await llenarSelect(
      document.querySelector('#select-donante'),
      '/donantes',
      (donante) => donante.razonSocial || `${donante.nombre || ''} ${donante.apellido || ''}`.trim(),
      'No hay donantes registrados'
    );
  }

  if (seccion === 'necesidades') {
    const selectEntidad = document.querySelector('#select-entidad');
    if (selectEntidad?.tagName === 'SELECT') {
      await llenarSelect(selectEntidad, '/beneficiarias', (entidad) => entidad.razonSocial, 'No hay entidades registradas');
      selectEntidad.addEventListener('change', () => cargarNecesidades(selectEntidad.value));
    } else if (selectEntidad?.value) {
      await cargarNecesidades(selectEntidad.value);
    }
  }

  if (seccion === 'matchmaking') {
    await llenarSelect(
      document.querySelector('#select-donacion'),
      '/donaciones?estado=EN_DEPOSITO',
      (donacion) => (donacion.bienes || []).map((bien) => `${bien.cantidad} ${String(bien.unidad).toLowerCase()} de ${bien.subcategoria}`).join(', ') || 'Donación sin bienes',
      'No hay donaciones en depósito'
    );
  }
}

async function llenarSelect(select, path, texto, vacio) {
  if (!select || select.tagName !== 'SELECT') return;
  try {
    const items = await request(apiUrl(path));
    const placeholder = document.createElement('option');
    placeholder.value = '';
    placeholder.textContent = items.length ? 'Elegí una opción' : vacio;
    select.replaceChildren(placeholder, ...items.map((item) => {
      const option = document.createElement('option');
      option.value = item.id;
      option.textContent = texto(item);
      return option;
    }));
  } catch (error) {
    showAlert(error.message);
  }
}

function cargarPagina(seccion) {
  if (seccion === 'donaciones') cargarDonaciones();
  if (seccion === 'logistica') cargarLogistica();
  if (seccion === 'catalogo') cargarCatalogo();
  if (seccion === 'matchmaking') cargarMatchmaking();
}
