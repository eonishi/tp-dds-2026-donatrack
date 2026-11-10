import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const source = fs.readFileSync(new URL('../../main/resources/public/panel/donaciones.js', import.meta.url), 'utf8')
  .replace(/^import .*;\n/gm, '').replaceAll('export ', '');
const fixture = {
  id: 'donacion-123', donanteId: 'donante-1', estado: 'EN_DEPOSITO', entidadBeneficiariaId: null,
  bienes: [
    { descripcion: '<img src=x onerror=alert(1)>', cantidad: 2, unidad: 'UNIDADES', subcategoria: 'Sillas', categoria: 'MOBILIARIO', estadoDeUso: 'USADO' },
    { descripcion: 'Otra silla', cantidad: 3, unidad: 'KILOGRAMOS', subcategoria: 'Sillas', categoria: 'MOBILIARIO', estadoDeUso: 'USADO' }
  ], fotos: []
};
function setup(request = async () => fixture) {
  const content = { innerHTML: '' };
  const title = { textContent: '' };
  const dialog = {
    open: false, showModal() { this.open = true; },
    querySelector: (selector) => selector === '[data-detail-title]' ? title : content
  };
  const container = { innerHTML: '', querySelectorAll: () => [] };
  const alerts = [];
  const context = vm.createContext({
    document: { querySelector: (selector) => selector === '#detalle-donacion' ? dialog : container },
    apiUrl: (path) => `/api/v1${path}`, request, showAlert: (...args) => alerts.push(args),
    showTracking() {}, URL, fixture, prompt: () => null, confirm: () => false
  });
  vm.runInContext(source, context);
  return { run: (code) => vm.runInContext(code, context), content, dialog, title, container, alerts };
}

test('cards show useful labeled data, escape text, and never offer editing or combine units', () => {
  const { run } = setup();
  run("donantes.set('donante-1', 'Ana Pérez')");
  const html = run('crearTarjeta(fixture)');
  for (const value of ['Sillas', 'Ana Pérez', 'Sin asignar', 'En depósito', 'Usado', '2 unidades', '3 kilogramos', 'Ver detalle', 'Historial']) assert.ok(html.includes(value), value);
  assert.ok(html.includes('&lt;img'));
  assert.ok(!html.includes('<img'));
  assert.ok(!html.includes('data-edit'));
  assert.ok(!html.includes('5 unidades'));
});

test('detail opens a readable dialog with all goods, not raw JSON', async () => {
  const { run, content, dialog, title } = setup();
  await run("mostrarDetalle({currentTarget:{dataset:{detail:'donacion-123'}}})");
  assert.equal(dialog.open, true);
  assert.equal(title.textContent, 'Detalle de la donación');
  for (const value of ['donacion-123', 'Bienes incluidos (2)', 'Otra silla', '3 kilogramos', 'Sin informar']) {
    assert.ok(content.innerHTML.includes(value), value);
  }
  assert.ok(content.innerHTML.includes('&lt;img'));
  assert.ok(!content.innerHTML.includes('"donanteId"'));
});

test('detail and history request errors are caught and shown inside dialog', async () => {
  const { run, content } = setup(async () => { throw new Error('<error>'); });
  await run("mostrarDetalle({currentTarget:{dataset:{detail:'missing'}}})");
  assert.ok(content.innerHTML.includes('No se pudo cargar el detalle: &lt;error&gt;'));
  await run("mostrarHistorial({currentTarget:{dataset:{history:'missing'}}})");
  assert.ok(content.innerHTML.includes('No se pudo cargar el historial: &lt;error&gt;'));
});

test('history renders human states, timestamps and escaped reasons', async () => {
  const { run, content } = setup(async () => [{ estadoAlcanzado: 'ENTREGA_FALLIDA', fecha: '2026-11-07T12:00', justificacion: '<motivo>' }]);
  await run("mostrarHistorial({currentTarget:{dataset:{history:'donacion-123'}}})");
  assert.ok(content.innerHTML.includes('Entrega fallida'));
  assert.ok(content.innerHTML.includes('2026-11-07 12:00'));
  assert.ok(content.innerHTML.includes('&lt;motivo&gt;'));
});

test('optional name lookup failures do not prevent loading donations', async () => {
  const { run, container, alerts } = setup(async (url) => {
    if (url === '/api/v1/donaciones') return [fixture];
    throw new Error('Forbidden');
  });
  await run('cargarDonaciones()');
  assert.ok(container.innerHTML.includes('Sillas'));
  assert.ok(container.innerHTML.includes('Donante donante-'));
  assert.equal(alerts.length, 0);
});

test('late detail response does not replace a newer history view', async () => {
  let resolveDetail;
  const { run, content } = setup((url) => url.endsWith('/historial-estados') ? Promise.resolve([]) : new Promise((resolve) => { resolveDetail = resolve; }));
  const pending = run("mostrarDetalle({currentTarget:{dataset:{detail:'old'}}})");
  await run("mostrarHistorial({currentTarget:{dataset:{history:'new'}}})");
  resolveDetail(fixture);
  await pending;
  assert.ok(content.innerHTML.includes('No hay cambios de estado'));
});


test('photo links only accept web URLs and escape attributes', () => {
  const { run } = setup();
  assert.equal(run("enlaceFoto('javascript:alert(1)', 0)"), '');
  assert.equal(run("enlaceFoto('not-a-url', 0)"), '');
  assert.ok(run("enlaceFoto('https://example.com/foto', 0)").includes('rel="noopener noreferrer"'));
});

test('starting transit sends the required truck id', async () => {
  let payload;
  const { run } = setup(async (url, options) => {
    if (options?.method === 'PATCH') { payload = JSON.parse(options.body); return {}; }
    return [];
  });
  run("globalThis.answers = ['en_traslado', 'ABC123']; prompt = () => answers.shift()");
  await run("cambiarEstado({currentTarget:{dataset:{state:'donacion-123'}}})");
  assert.deepEqual(payload, { estado: 'EN_TRASLADO', idCamion: 'ABC123' });
});

test('cards and detail render Jackson LocalDate arrays from the real API', async () => {
  const donation = { ...fixture, bienes: [{ ...fixture.bienes[0], fechaVencimiento: [2026, 12, 7] }] };
  const { run, container, content, alerts } = setup(async (url) => url.endsWith('/donaciones') ? [donation] : url.endsWith('/donacion-123') ? donation : []);
  await run('cargarDonaciones()');
  assert.ok(container.innerHTML.includes('07/12/2026'));
  assert.equal(alerts.length, 0);
  await run("mostrarDetalle({currentTarget:{dataset:{detail:'donacion-123'}}})");
  assert.ok(content.innerHTML.includes('07/12/2026'));
});

test('history renders Jackson LocalDateTime arrays including omitted seconds and nanoseconds', async () => {
  const { run, content } = setup(async () => [
    { estadoAlcanzado: 'EN_DEPOSITO', fecha: [2026, 11, 7, 9, 5] },
    { estadoAlcanzado: 'ENTREGADA', fecha: [2026, 11, 7, 12, 30, 45, 123456789] }
  ]);
  await run("mostrarHistorial({currentTarget:{dataset:{history:'donacion-123'}}})");
  assert.ok(content.innerHTML.includes('2026-11-07 09:05'));
  assert.ok(content.innerHTML.includes('2026-11-07 12:30:45'));
});

test('date formatting accepts ISO strings and safely handles absent or malformed dates', () => {
  const { run } = setup();
  assert.equal(run("fecha('2026-12-07')"), '07/12/2026');
  assert.equal(run("fecha('2026-12-07T12:30:00')"), '07/12/2026');
  assert.equal(run('fecha([2028, 2, 29])'), '29/02/2028');
  for (const value of ['null', '{}', '123', '[]', '[2026, 2, 30]', '[2026, 13, 1]', '[2026, 1]', "'<script>'"]) {
    assert.equal(run(`fecha(${value})`), 'Sin informar', value);
    assert.equal(run(`fechaHora(${value})`), 'Sin fecha', value);
  }
});
