import { apiUrl, request } from './api.js';
import { showAlert, showTracking } from './ui.js';

const container = document.querySelector('#donaciones-lista');
const dialog = document.querySelector('#detalle-donacion');
let donantes = new Map();
let entidades = new Map();
let detailRequest = 0;
const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]));
const etiquetas = { EN_DEPOSITO: 'En depósito', ASIGNACION_REALIZADA: 'Asignada', LISTA_PARA_ENTREGAR: 'Lista para entregar', EN_TRASLADO: 'En traslado', ENTREGADA: 'Entregada', ENTREGA_FALLIDA: 'Entrega fallida', VENCIDA: 'Vencida' };
const etiqueta = (value) => etiquetas[value] || (value ? String(value).toLowerCase().replaceAll('_', ' ').replace(/^./, (char) => char.toUpperCase()) : 'Sin informar');
// Java time values arrive as Jackson arrays; accept ISO strings as well.
function fechaNormalizada(value) {
  let parts;
  if (Array.isArray(value)) {
    if (![3, 5, 6, 7].includes(value.length) || !value.every(Number.isInteger)) return null;
    parts = value;
  } else if (typeof value === 'string') {
    const match = /^(\d{4})-(\d{2})-(\d{2})(?:T(\d{2}):(\d{2})(?::(\d{2})(?:\.\d{1,9})?)?(?:Z|[+-]\d{2}:\d{2})?)?$/.exec(value);
    if (!match) return null;
    parts = match.slice(1).filter((part) => part !== undefined).map(Number);
  } else return null;
  const [year, month, day, hour, minute, second, nanos] = parts;
  const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
  const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  if (year < 1 || year > 9999 || month < 1 || month > 12 || day < 1 || day > days[month - 1]) return null;
  if (parts.length > 3 && (hour < 0 || hour > 23 || minute < 0 || minute > 59 || (second !== undefined && (second < 0 || second > 59)) || (nanos !== undefined && (nanos < 0 || nanos > 999999999)))) return null;
  const pad = (number) => String(number).padStart(2, '0');
  const date = `${String(year).padStart(4, '0')}-${pad(month)}-${pad(day)}`;
  return parts.length === 3 ? date : `${date}T${pad(hour)}:${pad(minute)}${second === undefined ? '' : `:${pad(second)}`}`;
}
const fecha = (value) => {
  const normalizada = fechaNormalizada(value);
  return normalizada ? normalizada.split('T')[0].split('-').reverse().join('/') : 'Sin informar';
};
const fechaHora = (value) => fechaNormalizada(value)?.replace('T', ' ') || 'Sin fecha';
const cantidad = (bien) => `${escapeHtml(bien.cantidad)} ${escapeHtml(etiqueta(bien.unidad).toLowerCase())}`;
const nombreDonante = (id) => donantes.get(id) || `Donante ${String(id || '').slice(0, 8) || 'sin informar'}`;
const nombreEntidad = (id) => id == null ? 'Sin asignar' : entidades.get(id) || `Entidad #${id}`;

async function cargarNombres() {
  const resultados = await Promise.allSettled([request(apiUrl('/donantes')), request(apiUrl('/beneficiarias'))]);
  if (resultados[0].status === 'fulfilled') donantes = new Map(resultados[0].value.map((d) => [d.id, d.razonSocial || `${d.nombre || ''} ${d.apellido || ''}`.trim()]));
  if (resultados[1].status === 'fulfilled') entidades = new Map(resultados[1].value.map((e) => [e.id, e.razonSocial]));
}

function enlaceFoto(url, indice) {
  try {
    const parsed = new URL(url);
    if (!['https:', 'http:'].includes(parsed.protocol)) return '';
    return `<li><a href="${escapeHtml(parsed.href)}" target="_blank" rel="noopener noreferrer">Ver foto ${indice + 1}</a></li>`;
  } catch { return ''; }
}

function resumenBienes(bienes, limite = bienes.length) {
  return `<ul class="list-unstyled mb-0">${bienes.slice(0, limite).map((bien) => `<li class="mb-2 text-break"><strong>${escapeHtml(bien.descripcion || bien.subcategoria)}</strong><br><span class="text-body-secondary">${cantidad(bien)}</span></li>`).join('')}</ul>${bienes.length > limite ? `<p class="small text-body-secondary mb-0">Y ${bienes.length - limite} bienes más. Consultá el detalle.</p>` : ''}`;
}


export async function cargarDonaciones() {
  try {
    const [donaciones] = await Promise.all([request(apiUrl('/donaciones')), cargarNombres()]);
    container.innerHTML = donaciones.length ? donaciones.map(crearTarjeta).join('') : '<p class="text-body-secondary">No hay donaciones.</p>';
    registrarEventos();
  } catch (error) {
    container.innerHTML = '';
    showAlert(error.message);
  }
}

function crearTarjeta(donacion) {
  const id = escapeHtml(donacion.id);
  const bienes = donacion.bienes || [];
  const primero = bienes[0] || {};
  const botones = `
    <button type="button" class="btn btn-sm btn-primary" data-detail="${id}">Ver detalle</button>
    <button type="button" class="btn btn-sm btn-outline-secondary" data-history="${id}">Historial</button>
    ${donacion.estado === 'EN_TRASLADO' ? `<button type="button" class="btn btn-sm btn-outline-primary" data-track="${id}">Ver camión</button>` : ''}
    <button type="button" class="btn btn-sm btn-outline-secondary" data-photo="${id}">Agregar foto</button>
    <button type="button" class="btn btn-sm btn-outline-secondary" data-state="${id}">Cambiar estado</button>
    <button type="button" class="btn btn-sm btn-outline-danger" data-delete-donation="${id}">Eliminar</button>`;
  return `<div class="col-md-6 col-xl-4"><article class="tarjeta p-4 h-100 d-flex flex-column">
    <div class="d-flex justify-content-between align-items-start gap-2 mb-2"><h3 class="h5 text-break mb-0">${escapeHtml(primero.subcategoria || 'Donación sin bienes')}</h3><span class="badge text-bg-light border">${escapeHtml(etiqueta(donacion.estado))}</span></div>
    <p class="small text-body-secondary">${escapeHtml(etiqueta(primero.categoria))} · Ref. ${escapeHtml(String(donacion.id).slice(0, 8))}</p>
    <dl class="small"><dt>Donante</dt><dd class="text-break">${escapeHtml(nombreDonante(donacion.donanteId))}</dd><dt>Destino</dt><dd class="text-break">${escapeHtml(nombreEntidad(donacion.entidadBeneficiariaId))}</dd>
    ${primero.estadoDeUso && primero.estadoDeUso !== 'NO_APLICA' ? `<dt>Condición</dt><dd>${escapeHtml(etiqueta(primero.estadoDeUso))}</dd>` : ''}
    ${primero.fechaVencimiento ? `<dt>Vencimiento</dt><dd>${fecha(primero.fechaVencimiento)}</dd>` : ''}</dl>
    <h4 class="h6">Bienes (${bienes.length})</h4>${resumenBienes(bienes, 3)}
    <div class="mt-auto pt-3 d-flex flex-wrap gap-2">${botones}</div></article></div>`;
}

function registrarEventos() {
  container.querySelectorAll('[data-detail]').forEach((b) => b.addEventListener('click', mostrarDetalle));
  container.querySelectorAll('[data-history]').forEach((b) => b.addEventListener('click', mostrarHistorial));
  container.querySelectorAll('[data-track]').forEach((b) => b.addEventListener('click', mostrarSeguimiento));
  container.querySelectorAll('[data-photo]').forEach((b) => b.addEventListener('click', agregarFoto));
  container.querySelectorAll('[data-state]').forEach((b) => b.addEventListener('click', cambiarEstado));
  container.querySelectorAll('[data-delete-donation]').forEach((b) => b.addEventListener('click', eliminarDonacion));
}

function abrirDetalle(titulo) {
  dialog.querySelector('[data-detail-title]').textContent = titulo;
  dialog.querySelector('[data-detail-content]').innerHTML = '<p role="status">Cargando…</p>';
  if (!dialog.open) dialog.showModal();
  return ++detailRequest;
}

async function mostrarDetalle(e) {
  const version = abrirDetalle('Detalle de la donación');
  try {
    const donacion = await request(apiUrl(`/donaciones/${encodeURIComponent(e.currentTarget.dataset.detail)}`));
    if (version !== detailRequest) return;
    const bienes = donacion.bienes || [];
    dialog.querySelector('[data-detail-content]').innerHTML = `
      <dl><dt>Referencia</dt><dd class="text-break">${escapeHtml(donacion.id)}</dd><dt>Estado</dt><dd>${escapeHtml(etiqueta(donacion.estado))}</dd>
      <dt>Donante</dt><dd>${escapeHtml(nombreDonante(donacion.donanteId))}</dd><dt>Destino</dt><dd>${escapeHtml(nombreEntidad(donacion.entidadBeneficiariaId))}</dd></dl>
      <h3 class="h5">Bienes incluidos (${bienes.length})</h3>
      ${bienes.map((bien) => `<section class="border rounded p-3 mb-3"><h4 class="h6 text-break">${escapeHtml(bien.descripcion)}</h4><dl class="mb-0"><dt>Cantidad</dt><dd>${cantidad(bien)}</dd><dt>Subcategoría / categoría</dt><dd>${escapeHtml(bien.subcategoria)} / ${escapeHtml(etiqueta(bien.categoria))}</dd><dt>Condición</dt><dd>${escapeHtml(bien.estadoDeUso === 'NO_APLICA' ? 'No aplica' : etiqueta(bien.estadoDeUso))}</dd><dt>Vencimiento</dt><dd>${fecha(bien.fechaVencimiento)}</dd></dl></section>`).join('') || '<p>Sin bienes registrados.</p>'}
      <h3 class="h5">Fotos</h3><ul>${[...(donacion.fotos || []), ...bienes.map((b) => b.foto).filter(Boolean)].map(enlaceFoto).join('') || '<li>Sin fotos disponibles.</li>'}</ul>`;
  } catch (error) {
    if (version === detailRequest) dialog.querySelector('[data-detail-content]').innerHTML = `<p class="alert alert-danger" role="alert">No se pudo cargar el detalle: ${escapeHtml(error.message)}</p>`;
  }
}

async function mostrarHistorial(e) {
  const version = abrirDetalle('Historial de la donación');
  try {
    const historial = await request(apiUrl(`/donaciones/${encodeURIComponent(e.currentTarget.dataset.history)}/historial-estados`));
    if (version !== detailRequest) return;
    dialog.querySelector('[data-detail-content]').innerHTML = historial.length ? `<ol>${historial.map((h) => `<li class="mb-3"><strong>${escapeHtml(etiqueta(h.estadoAlcanzado))}</strong><br><time>${fechaHora(h.fecha)}</time>${h.justificacion ? `<p class="text-break mb-0">${escapeHtml(h.justificacion)}</p>` : ''}</li>`).join('')}</ol>` : '<p>No hay cambios de estado registrados.</p>';
  } catch (error) {
    if (version === detailRequest) dialog.querySelector('[data-detail-content]').innerHTML = `<p class="alert alert-danger" role="alert">No se pudo cargar el historial: ${escapeHtml(error.message)}</p>`;
  }
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
  const transicion = { estado: estado.trim().toUpperCase() };
  if (transicion.estado === 'EN_TRASLADO') {
    transicion.idCamion = prompt('Patente del camión');
    if (!transicion.idCamion?.trim()) return;
  }
  if (transicion.estado === 'ENTREGA_FALLIDA') {
    transicion.justificacion = prompt('Motivo de la entrega fallida');
    if (!transicion.justificacion?.trim()) return;
  }
  try {
    await request(apiUrl(`/donaciones/${e.currentTarget.dataset.state}/estado`), { method: 'PATCH', body: JSON.stringify(transicion) });
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
  const form = document.querySelector('#form-donacion');
  if (!form) return;
  const items = form.querySelector('#bienes-carga');
  const template = document.querySelector('#bien-carga-template');
  const actualizarBotones = () => items.querySelectorAll('[data-quitar-bien]').forEach((button) => {
    button.disabled = items.children.length === 1;
  });
  const agregarBien = () => {
    const item = template.content.firstElementChild.cloneNode(true);
    const categoria = item.querySelector('[name="categoria"]');
    const actualizarCampos = () => {
      const requiereEstado = ['MOBILIARIO', 'VESTIMENTA'].includes(categoria.value);
      const estado = item.querySelector('[name="estadoDeUso"]');
      estado.disabled = !requiereEstado;
      estado.required = requiereEstado;
      item.querySelector('[data-estado-container]').hidden = !requiereEstado;
      const vencimiento = item.querySelector('[name="fechaVencimiento"]');
      vencimiento.disabled = requiereEstado;
      item.querySelector('[data-vencimiento-container]').hidden = requiereEstado;
      if (requiereEstado) vencimiento.value = '';
      else estado.value = '';
    };
    categoria.addEventListener('change', actualizarCampos);
    item.querySelector('[data-quitar-bien]').addEventListener('click', () => {
      if (items.children.length > 1) item.remove();
      actualizarBotones();
    });
    actualizarCampos();
    items.append(item);
    actualizarBotones();
  };
  form.querySelector('#agregar-bien').addEventListener('click', agregarBien);
  agregarBien();
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!form.reportValidity()) return;
    const bienes = Array.from(items.querySelectorAll('[data-bien]'), (item) => {
      const value = (name) => item.querySelector(`[name="${name}"]`).value;
      return {
        descripcion: value('descripcion'), cantidad: Number(value('cantidad')),
        unidad: value('unidad'), categoria: value('categoria'), subcategoria: value('subcategoria'),
        estadoDeUso: value('categoria') === 'ALIMENTO' ? 'NO_APLICA' : value('estadoDeUso'),
        fechaVencimiento: value('fechaVencimiento') || null
      };
    });
    const submit = form.querySelector('[type="submit"]');
    submit.disabled = true;
    try {
      const creadas = await request(apiUrl('/donaciones'), {
        method: 'POST', body: JSON.stringify({ donanteId: new FormData(form).get('donanteId'), bienes })
      });
      form.reset();
      items.replaceChildren();
      agregarBien();
      form.classList.add('d-none');
      await cargarDonaciones();
      showAlert(`Carga registrada: ${creadas.length} donaciones generadas.`, 'success');
    } catch (error) { showAlert(error.message); }
    finally { submit.disabled = false; }
  });
}
