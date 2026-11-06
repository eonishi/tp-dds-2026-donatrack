(function () {
  const root = document.querySelector('.panel-app');
  if (!root) return;
  const base = '/api/v1';
  const logisticsBase = `${window.location.protocol}//${window.location.hostname}:9001/logistica`;
  const alertBox = document.querySelector('#panel-alert');

  function showAlert(message, kind = 'danger') {
    alertBox.textContent = message;
    alertBox.className = `alert alert-${kind}`;
  }

  async function request(url, options = {}) {
    const response = await fetch(url, { credentials: 'include', ...options, headers: { 'Content-Type': 'application/json', ...(options.headers || {}) } });
    if (!response.ok) throw new Error((await response.text()) || `Error HTTP ${response.status}`);
    return response.status === 204 ? null : response.json();
  }

  // Muestra la última posición del camión con un enlace al mapa. Se arma con nodos y textContent
  // para no interpretar como HTML nada de lo que devuelve la API.
  function showTracking(posicion) {
    const lat = Number(posicion.latitud);
    const lng = Number(posicion.longitud);
    const link = document.createElement('a');
    link.href = `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lng}#map=15/${lat}/${lng}`;
    link.target = '_blank';
    link.rel = 'noopener';
    link.textContent = 'Ver en el mapa';
    alertBox.replaceChildren(`Camión ${posicion.patente}: ${lat.toFixed(5)}, ${lng.toFixed(5)} · actualizado ${posicion.actualizadaEn || 'sin fecha'} · `, link);
    alertBox.className = 'alert alert-info';
  }

  function card(title, body, actions = '') {
    return `<div class="col-md-6 col-xl-4"><article class="tarjeta p-4 h-100"><h3 class="h5">${title}</h3><div class="small text-body-secondary">${body}</div>${actions ? `<div class="mt-3 d-flex flex-wrap gap-2">${actions}</div>` : ''}</article></div>`;
  }

  async function loadDonaciones() {
    const container = document.querySelector('#donaciones-lista');
    try {
      const items = await request(`${base}/donaciones`);
      container.innerHTML = items.length ? items.map(d => card(d.id, `Estado: <strong>${d.estado}</strong><br>Entidad: ${d.entidadBeneficiariaId || 'sin asignar'}<br>Bienes: ${d.bienes?.length || 0}<br>Fotos: ${d.fotos?.length || 0}`, `<button class="btn btn-sm btn-outline-primary" data-detail="${d.id}">Detalle</button><button class="btn btn-sm btn-outline-secondary" data-edit="${d.id}">Editar</button><button class="btn btn-sm btn-outline-secondary" data-history="${d.id}">Historial</button>${d.estado === 'EN_TRASLADO' ? `<button class="btn btn-sm btn-primary" data-track="${d.id}">Ver camión</button>` : ''}<button class="btn btn-sm btn-outline-secondary" data-photo="${d.id}">Foto</button><button class="btn btn-sm btn-outline-secondary" data-state="${d.id}">Estado</button><button class="btn btn-sm btn-outline-danger" data-delete-donation="${d.id}">Eliminar</button>`)).join('') : '<p class="text-body-secondary">No hay donaciones.</p>';
      container.querySelectorAll('[data-detail]').forEach(b => b.onclick = async () => showAlert(JSON.stringify(await request(`${base}/donaciones/${b.dataset.detail}`), null, 2), 'info'));
      container.querySelectorAll('[data-edit]').forEach(b => b.onclick = async () => { try { const d = await request(`${base}/donaciones/${b.dataset.edit}`); const descripcion = prompt('Descripción del bien', d.bienes?.[0]?.descripcion || ''); if (!descripcion) return; const bienes = (d.bienes || []).map((bien, i) => ({ ...bien, descripcion: i === 0 ? descripcion : bien.descripcion })); await request(`${base}/donaciones/${b.dataset.edit}`, { method: 'PUT', body: JSON.stringify({ bienes }) }); await loadDonaciones(); } catch (e) { showAlert(e.message); } });
      container.querySelectorAll('[data-history]').forEach(b => b.onclick = async () => showAlert(JSON.stringify(await request(`${base}/donaciones/${b.dataset.history}/historial-estados`), null, 2), 'info'));
      container.querySelectorAll('[data-track]').forEach(b => b.onclick = async () => { try { showTracking(await request(`${base}/donaciones/${b.dataset.track}/seguimiento`)); } catch (e) { showAlert(e.message); } });
      container.querySelectorAll('[data-photo]').forEach(b => b.onclick = async () => { const url = prompt('URL de la foto'); if (!url) return; try { await request(`${base}/donaciones/${b.dataset.photo}/fotos`, { method: 'POST', body: JSON.stringify({ url }) }); await loadDonaciones(); } catch (e) { showAlert(e.message); } });
      container.querySelectorAll('[data-state]').forEach(b => b.onclick = async () => { const estado = prompt('Nuevo estado (LISTA_PARA_ENTREGAR, EN_TRASLADO, ENTREGADA, ENTREGA_FALLIDA, VENCIDA, EN_DEPOSITO)'); if (!estado) return; try { await request(`${base}/donaciones/${b.dataset.state}/estado`, { method: 'PATCH', body: JSON.stringify({ estado }) }); await loadDonaciones(); } catch (e) { showAlert(e.message); } });
      container.querySelectorAll('[data-delete-donation]').forEach(b => b.onclick = async () => { if (!confirm('¿Eliminar esta donación?')) return; try { await request(`${base}/donaciones/${b.dataset.deleteDonation}`, { method: 'DELETE' }); await loadDonaciones(); } catch (e) { showAlert(e.message); } });
    } catch (e) { container.innerHTML = ''; showAlert(e.message); }
  }

  async function loadNecesidades(entidadId) {
    if (!entidadId) return;
    const container = document.querySelector('#necesidades-lista');
    try {
      const items = await request(`${base}/beneficiarias/${encodeURIComponent(entidadId)}/necesidades`);
      container.innerHTML = items.length ? items.map(n => card(n.id, `Tipo: ${n.tipo}<br>${n.descripcion}<br>Objetivo: ${n.cantidadObjetivo} ${n.unidad}`, `<button class="btn btn-sm btn-outline-danger" data-delete-need="${n.id}">Eliminar</button>`)).join('') : '<p class="text-body-secondary">No hay necesidades cargadas.</p>';
      container.querySelectorAll('[data-delete-need]').forEach(b => b.onclick = async () => { try { await request(`${base}/beneficiarias/${encodeURIComponent(entidadId)}/necesidades/${b.dataset.deleteNeed}`, { method: 'DELETE' }); await loadNecesidades(entidadId); } catch (e) { showAlert(e.message); } });
    } catch (e) { showAlert(e.message); }
  }

  async function loadLogistica() {
    try {
      const [camiones, rutas] = await Promise.all([request(`${logisticsBase}/camiones`), request(`${logisticsBase}/rutas`)]);
      document.querySelector('#camiones-lista').innerHTML = camiones.length ? camiones.map(c => `<p class="mb-2"><strong>${c.patente}</strong><br><span class="small text-body-secondary">${c.capacidadCargaKg} kg · ${c.volumenM3} m³</span></p>`).join('') : '<p class="text-body-secondary">No hay camiones.</p>';
      document.querySelector('#rutas-lista').innerHTML = rutas.length ? rutas.map(r => card(r.id, `Camión: ${r.camion?.patente || 'sin patente'}<br>Estado: ${r.estado || 'pendiente'}`, `<button class="btn btn-sm btn-primary" data-route-start="${r.id}">Iniciar</button><button class="btn btn-sm btn-outline-secondary" data-route-finish="${r.id}">Finalizar</button>`)).join('') : '<p class="text-body-secondary">No hay rutas.</p>';
      document.querySelectorAll('[data-route-start], [data-route-finish]').forEach(b => b.onclick = async () => { const estado = b.dataset.routeStart ? 'EN_CURSO' : 'FINALIZADA'; try { await request(`${logisticsBase}/rutas/${b.dataset.routeStart || b.dataset.routeFinish}`, { method: 'PATCH', body: JSON.stringify({ estado }) }); await loadLogistica(); } catch (e) { showAlert(e.message); } });
    } catch (e) { showAlert(`No se pudo consultar logística: ${e.message}`); }
  }

  async function loadCatalogo() {
    const container = document.querySelector('#catalogo-lista');
    try {
      const items = await request(`${base}/subcategorias`);
      container.innerHTML = items.length ? items.map(s => card(s.nombre, `Categoría: ${s.categoria}`)).join('') : '<p class="text-body-secondary">No hay subcategorías.</p>';
    } catch (e) { showAlert(e.message); }
  }

  async function loadMatchmaking() {
    const container = document.querySelector('#matchmaking-lista');
    try {
      const items = await request(`${base}/propuestas-asignacion`);
      container.innerHTML = items.length ? items.map(p => card(p.donacionId, `Pendiente: ${p.pendiente}<br>${(p.rankings || []).map(r => `<strong>${r.algoritmo}</strong>: ${(r.candidatos || []).map(c => `${c.razonSocial} (${c.puntaje})`).join(', ')}`).join('<br>')}`, (p.rankings?.[0]?.candidatos?.[0] ? `<button class="btn btn-sm btn-primary" data-select-match="${p.donacionId}" data-entity-id="${p.rankings[0].candidatos[0].entidadBeneficiariaId}">Seleccionar mejor candidato</button>` : '') )).join('') : '<p class="text-body-secondary">No hay propuestas pendientes.</p>';
      container.querySelectorAll('[data-select-match]').forEach(b => b.onclick = async () => { try { await request(`${base}/donaciones/${b.dataset.selectMatch}/propuesta-asignacion`, { method: 'PUT', body: JSON.stringify({ entidadBeneficiariaId: Number(b.dataset.entityId) }) }); await loadMatchmaking(); } catch (e) { showAlert(e.message); } });
    } catch (e) { showAlert(e.message); }
  }

  document.querySelector('[data-action="mostrar-form-donacion"]')?.addEventListener('click', () => document.querySelector('#form-donacion').classList.remove('d-none'));
  document.querySelector('[data-action="ocultar-form-donacion"]')?.addEventListener('click', () => document.querySelector('#form-donacion').classList.add('d-none'));
  document.querySelector('[data-action="recargar-logistica"]')?.addEventListener('click', loadLogistica);
  document.querySelector('[data-action="recargar-matchmaking"]')?.addEventListener('click', loadMatchmaking);
  document.querySelector('#form-donacion')?.addEventListener('submit', async e => { e.preventDefault(); const f = new FormData(e.target); try { await request(`${base}/donaciones`, { method: 'POST', body: JSON.stringify({ donanteId: f.get('donanteId'), bienes: [{ descripcion: f.get('descripcion'), cantidad: Number(f.get('cantidad')), unidad: f.get('unidad'), estadoDeUso: 'NO_APLICA', categoria: f.get('categoria'), subcategoria: f.get('subcategoria') }] }) }); e.target.reset(); e.target.classList.add('d-none'); await loadDonaciones(); } catch (err) { showAlert(err.message); } });
  document.querySelector('#form-necesidad')?.addEventListener('submit', async e => { e.preventDefault(); const f = new FormData(e.target); try { await request(`${base}/beneficiarias/${encodeURIComponent(f.get('entidadId'))}/necesidades`, { method: 'POST', body: JSON.stringify({ tipo: f.get('tipo'), subcategoria: f.get('subcategoria'), categoria: f.get('categoria'), cantidadObjetivo: Number(f.get('cantidadObjetivo')), unidad: f.get('unidad'), descripcion: f.get('descripcion') }) }); await loadNecesidades(f.get('entidadId')); } catch (err) { showAlert(err.message); } });
  document.querySelector('#form-camion')?.addEventListener('submit', async e => { e.preventDefault(); const f = new FormData(e.target); try { await request(`${logisticsBase}/camiones`, { method: 'POST', body: JSON.stringify({ patente: f.get('patente'), volumenM3: Number(f.get('volumenM3')), alturaM: Number(f.get('alturaM')), capacidadCargaKg: Number(f.get('capacidadCargaKg')) }) }); e.target.reset(); await loadLogistica(); } catch (err) { showAlert(err.message); } });
  document.querySelector('#form-subcategoria')?.addEventListener('submit', async e => { e.preventDefault(); const f = new FormData(e.target); try { await request(`${base}/subcategorias`, { method: 'POST', body: JSON.stringify({ nombre: f.get('nombre'), categoria: f.get('categoria') }) }); e.target.reset(); await loadCatalogo(); } catch (err) { showAlert(err.message); } });
  document.querySelector('#form-matchmaking')?.addEventListener('submit', async e => { e.preventDefault(); const id = new FormData(e.target).get('donacionId'); try { await request(`${base}/donaciones/${encodeURIComponent(id)}/propuesta-asignacion`, { method: 'POST' }); await loadMatchmaking(); } catch (err) { showAlert(err.message); } });

  const section = root.dataset.seccion;
  document.querySelectorAll('.panel-vista').forEach(v => v.classList.add('d-none'));
  document.querySelector(`#vista-${section}`)?.classList.remove('d-none');
  if (section === 'donaciones') loadDonaciones();
  if (section === 'logistica') loadLogistica();
  if (section === 'catalogo') loadCatalogo();
  if (section === 'matchmaking') loadMatchmaking();
})();
