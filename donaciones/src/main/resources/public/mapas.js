// Mapas de DonaTrack (Leaflet + OpenStreetMap). Los datos llegan por JSON desde el servidor y los
// popups se arman con nodos y textContent: nada de lo que viene de la base se interpreta como HTML.
(function () {
  if (typeof L === 'undefined') return;
  const CENTRO = [-34.6037, -58.3816]; // Buenos Aires, si todavía no hay nada que mostrar
  const REFRESCO_MS = 30000;

  function nuevoMapa(elemento) {
    const mapa = L.map(elemento, { scrollWheelZoom: false }).setView(CENTRO, 11);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; colaboradores de OpenStreetMap'
    }).addTo(mapa);
    return mapa;
  }

  function nodo(tag, texto, clase) {
    const n = document.createElement(tag);
    if (texto) n.textContent = texto;
    if (clase) n.className = clase;
    return n;
  }

  function icono(emoji, titulo) {
    return L.divIcon({ className: 'marcador-emoji', html: `<span role="img" aria-label="${titulo}">${emoji}</span>`, iconSize: [32, 32], iconAnchor: [16, 16] });
  }

  function ajustar(mapa, puntos) {
    if (puntos.length === 1) mapa.setView(puntos[0], 14);
    else if (puntos.length > 1) mapa.fitBounds(puntos, { padding: [40, 40] });
  }

  function avisar(elemento, mensaje) {
    const aviso = elemento.parentElement.querySelector('[data-aviso-mapa]');
    if (aviso) { aviso.textContent = mensaje; aviso.classList.toggle('d-none', !mensaje); }
  }

  // --- Mapa público de entregas: un marcador por entidad con lo que recibió ---
  async function mapaEntregas(elemento) {
    const mapa = nuevoMapa(elemento);
    try {
      const respuesta = await fetch(elemento.dataset.fuente, { headers: { Accept: 'application/json' } });
      if (!respuesta.ok) throw new Error();
      const marcadores = await respuesta.json();
      const puntos = [];
      marcadores.forEach(m => {
        const contenido = nodo('div', null, 'popup-entregas');
        contenido.append(nodo('strong', m.entidad));
        contenido.append(nodo('div', `${m.entregas.length} ${m.entregas.length === 1 ? 'entrega' : 'entregas'}`, 'small text-body-secondary mb-2'));
        m.entregas.slice(0, 5).forEach(e => {
          const bloque = nodo('div', null, 'mb-2');
          if (e.fecha) bloque.append(nodo('div', `Entregada el ${e.fecha}`, 'small fw-semibold'));
          const lista = nodo('ul', null, 'small mb-1 ps-3');
          e.bienes.forEach(b => lista.append(nodo('li', b)));
          bloque.append(lista);
          const fotos = nodo('div', null, 'd-flex gap-1 flex-wrap');
          e.fotos.slice(0, 4).forEach(url => {
            const enlace = nodo('a'); enlace.href = url; enlace.target = '_blank'; enlace.rel = 'noopener';
            const img = nodo('img'); img.src = url; img.alt = `Foto de la entrega a ${m.entidad}`; img.width = 56; img.height = 56; img.loading = 'lazy'; img.className = 'rounded-2'; img.style.objectFit = 'cover';
            enlace.append(img); fotos.append(enlace);
          });
          bloque.append(fotos);
          contenido.append(bloque);
        });
        L.marker([m.latitud, m.longitud], { icon: icono('🎁', 'Entidad con entregas'), title: m.entidad, alt: m.entidad }).addTo(mapa).bindPopup(contenido, { maxWidth: 280 });
        puntos.push([m.latitud, m.longitud]);
      });
      ajustar(mapa, puntos);
      avisar(elemento, marcadores.length ? '' : 'Todavía no hay entregas para mostrar en el mapa.');
    } catch (e) {
      avisar(elemento, 'No pudimos cargar el mapa. Probá de nuevo en un rato.');
    }
  }

  // --- Seguimiento: el camión de cada donación en camino y su destino, actualizado cada 30 s ---
  function mapaSeguimiento(elemento) {
    const mapa = nuevoMapa(elemento);
    const capa = L.layerGroup().addTo(mapa);
    let primeraVez = true;

    async function actualizar() {
      try {
        const respuesta = await fetch(elemento.dataset.fuente, { headers: { Accept: 'application/json' }, credentials: 'same-origin' });
        if (respuesta.status === 401 || respuesta.redirected) { avisar(elemento, 'Tu sesión venció. Volvé a ingresar.'); return; }
        if (!respuesta.ok) throw new Error();
        const entregas = await respuesta.json();
        capa.clearLayers();
        const puntos = [];
        entregas.forEach(e => {
          if (e.latitud != null && e.longitud != null) {
            const contenido = nodo('div');
            contenido.append(nodo('strong', `Camión ${e.camion}`));
            contenido.append(nodo('div', `Última ubicación: ${e.actualizadaEn || 'sin hora'}`, 'small'));
            contenido.append(nodo('div', `Lleva: ${e.bienes.join(', ')}`, 'small'));
            if (e.entidad) contenido.append(nodo('div', `Destino: ${e.entidad}`, 'small'));
            L.marker([e.latitud, e.longitud], { icon: icono('🚚', 'Camión'), title: `Camión ${e.camion}`, alt: `Camión ${e.camion}` }).addTo(capa).bindPopup(contenido);
            puntos.push([e.latitud, e.longitud]);
          }
          if (e.destinoLatitud != null && e.destinoLongitud != null) {
            L.marker([e.destinoLatitud, e.destinoLongitud], { icon: icono('🏠', 'Destino'), title: e.entidad, alt: e.entidad }).addTo(capa).bindPopup(nodo('div', `Destino: ${e.entidad}`));
            puntos.push([e.destinoLatitud, e.destinoLongitud]);
            if (e.latitud != null) L.polyline([[e.latitud, e.longitud], [e.destinoLatitud, e.destinoLongitud]], { color: '#2e8b3d', dashArray: '6 8', weight: 2 }).addTo(capa);
          }
        });
        if (primeraVez) { ajustar(mapa, puntos); primeraVez = false; }
        const hora = new Date().toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit', hour12: false });
        avisar(elemento, entregas.length ? `Actualizado a las ${hora}. Se actualiza solo cada 30 segundos.` : 'No hay entregas en camino en este momento.');
      } catch (e) {
        avisar(elemento, 'No pudimos actualizar las ubicaciones. Reintentamos en 30 segundos.');
      }
    }

    actualizar();
    setInterval(actualizar, REFRESCO_MS);
  }

  document.querySelectorAll('[data-mapa="entregas"]').forEach(mapaEntregas);
  document.querySelectorAll('[data-mapa="seguimiento"]').forEach(mapaSeguimiento);
})();
