const alertBox = document.querySelector('#panel-alert');

export function showAlert(message, kind = 'danger') {
  alertBox.textContent = message;
  alertBox.className = `alert alert-${kind}`;
}

export function card(title, body, actions = '') {
  const botones = actions ? `<div class="mt-3 d-flex flex-wrap gap-2">${actions}</div>` : '';
  return `<div class="col-md-6 col-xl-4"><article class="tarjeta p-4 h-100"><h3 class="h5">${title}</h3><div class="small text-body-secondary">${body}</div>${botones}</article></div>`;
}

export function showTracking(posicion) {
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
