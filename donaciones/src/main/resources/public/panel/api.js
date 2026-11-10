const base = '/api/v1';
const logisticsBase = `${window.location.protocol}//${window.location.hostname}:9001/logistica`;

export async function request(url, options = {}) {
  const response = await fetch(url, {
    credentials: 'include',
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) }
  });
  if (!response.ok) throw new Error((await response.text()) || `Error HTTP ${response.status}`);
  return response.status === 204 ? null : response.json();
}

export function apiUrl(path) { return `${base}${path}`; }

export async function logistica(query, variables = {}) {
  const respuesta = await request(`${logisticsBase}/graphql`, {
    method: 'POST', body: JSON.stringify({ query, variables })
  });
  if (respuesta.errors?.length) throw new Error(respuesta.errors.map((e) => e.message).join(' · '));
  return respuesta.data;
}
