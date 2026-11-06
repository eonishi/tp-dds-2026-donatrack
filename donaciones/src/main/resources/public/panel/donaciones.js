import { apiUrl, request } from './api.js';
import { card, showAlert, showTracking } from './ui.js';

const container = document.querySelector('#donaciones-lista');

export async function cargarDonaciones() {
  try {
    const donaciones = await request(apiUrl('/donaciones'));
    container.innerHTML = donaciones.length ? donaciones.map(crearTarjeta).join('') : '<p class="text-body-secondary">No hay donaciones.</p>';
    registrarEventos();
  } catch (error) {
    container.innerHTML = '';
    showAlert(error.message);
  }
}

function crearTarjeta(donacion) {
  const botones = `
    <button class="btn btn-sm btn-outline-primary" data-detail="${donacion.id}">Detalle</button>
    <button class="btn btn-sm btn-outline-secondary" data-edit="${donacion.id}">Editar</button>
    <button class="btn btn-sm btn-outline-secondary" data-history="${donacion.id}">Historial</button>
    ${donacion.estado === 'EN_TRASLADO' ? `<button class="btn btn-sm btn-primary" data-track="${donacion.id}">Ver camión</button>` : ''}
    <button class="btn btn-sm btn-outline-secondary" data-photo="${donacion.id}">Foto</button>
    <button class="btn btn-sm btn-outline-secondary" data-state="${donacion.id}">Estado</button>
    <button class="btn btn-sm btn-outline-danger" data-delete-donation="${donacion.id}">Eliminar</button>`;
  const datos = `Estado: <strong>${donacion.estado}</strong><br>Entidad: ${donacion.entidadBeneficiariaId || 'sin asignar'}<br>Bienes: ${donacion.bienes?.length || 0}<br>Fotos: ${donacion.fotos?.length || 0}`;
  return card(donacion.id, datos, botones);
}

function registrarEventos() {
  container.querySelectorAll('[data-detail]').forEach((b) => b.addEventListener('click', mostrarDetalle));
  container.querySelectorAll('[data-edit]').forEach((b) => b.addEventListener('click', editarDonacion));
  container.querySelectorAll('[data-history]').forEach((b) => b.addEventListener('click', mostrarHistorial));
  container.querySelectorAll('[data-track]').forEach((b) => b.addEventListener('click', mostrarSeguimiento));
  container.querySelectorAll('[data-photo]').forEach((b) => b.addEventListener('click', agregarFoto));
  container.querySelectorAll('[data-state]').forEach((b) => b.addEventListener('click', cambiarEstado));
  container.querySelectorAll('[data-delete-donation]').forEach((b) => b.addEventListener('click', eliminarDonacion));
}

async function mostrarDetalle(e) { showAlert(JSON.stringify(await request(apiUrl(`/donaciones/${e.currentTarget.dataset.detail}`)), null, 2), 'info'); }
async function mostrarHistorial(e) { showAlert(JSON.stringify(await request(apiUrl(`/donaciones/${e.currentTarget.dataset.history}/historial-estados`)), null, 2), 'info'); }

async function editarDonacion(e) {
  try {
    const id = e.currentTarget.dataset.edit;
    const donacion = await request(apiUrl(`/donaciones/${id}`));
    const descripcion = prompt('Descripción del bien', donacion.bienes?.[0]?.descripcion || '');
    if (!descripcion) return;
    const bienes = (donacion.bienes || []).map((bien, i) => ({ ...bien, descripcion: i === 0 ? descripcion : bien.descripcion }));
    await request(apiUrl(`/donaciones/${id}`), { method: 'PUT', body: JSON.stringify({ bienes }) });
    await cargarDonaciones();
  } catch (error) { showAlert(error.message); }
}

async function mostrarSeguimiento(e) {
  try { showTracking(await request(apiUrl(`/donaciones/${e.currentTarget.dataset.track}/seguimiento`))); }
  catch (error) { showAlert(error.message); }
}

async function agregarFoto(e) {
  const url = prompt('URL de la foto');
  if (!url) return;
  try {
    await request(apiUrl(`/donaciones/${e.currentTarget.dataset.photo}/fotos`), { method: 'POST', body: JSON.stringify({ url }) });
    await cargarDonaciones();
  } catch (error) { showAlert(error.message); }
}

async function cambiarEstado(e) {
  const estado = prompt('Nuevo estado (LISTA_PARA_ENTREGAR, EN_TRASLADO, ENTREGADA, ENTREGA_FALLIDA, VENCIDA, EN_DEPOSITO)');
  if (!estado) return;
  try {
    await request(apiUrl(`/donaciones/${e.currentTarget.dataset.state}/estado`), { method: 'PATCH', body: JSON.stringify({ estado }) });
    await cargarDonaciones();
  } catch (error) { showAlert(error.message); }
}

async function eliminarDonacion(e) {
  if (!confirm('¿Eliminar esta donación?')) return;
  try {
    await request(apiUrl(`/donaciones/${e.currentTarget.dataset.deleteDonation}`), { method: 'DELETE' });
    await cargarDonaciones();
  } catch (error) { showAlert(error.message); }
}

export function registrarFormularioDonacion() {
  document.querySelector('#form-donacion')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const f = new FormData(e.target);
    try {
      await request(apiUrl('/donaciones'), { method: 'POST', body: JSON.stringify({ donanteId: f.get('donanteId'), bienes: [{ descripcion: f.get('descripcion'), cantidad: Number(f.get('cantidad')), unidad: f.get('unidad'), estadoDeUso: 'NO_APLICA', categoria: f.get('categoria'), subcategoria: f.get('subcategoria') }] }) });
      e.target.reset(); e.target.classList.add('d-none'); await cargarDonaciones();
    } catch (error) { showAlert(error.message); }
  });
}
