import { cargarDonaciones, registrarFormularioDonacion } from './panel/donaciones.js';
import { registrarFormularioNecesidad } from './panel/necesidades.js';
import { cargarLogistica, registrarFormularioCamion } from './panel/logistica.js';
import { cargarCatalogo, registrarFormularioSubcategoria } from './panel/catalogo.js';
import { cargarMatchmaking, registrarFormularioMatchmaking } from './panel/matchmaking.js';

const root = document.querySelector('.panel-app');

if (root) {
  prepararVista(root.dataset.seccion);
  registrarAccionesComunes();
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

function cargarPagina(seccion) {
  if (seccion === 'donaciones') cargarDonaciones();
  if (seccion === 'logistica') cargarLogistica();
  if (seccion === 'catalogo') cargarCatalogo();
  if (seccion === 'matchmaking') cargarMatchmaking();
}
