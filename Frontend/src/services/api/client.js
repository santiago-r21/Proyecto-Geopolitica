/**
 * Cliente HTTP mínimo para nuestro propio backend (Spring Boot).
 * El backend ya resuelve la OEC, el caché y los reintentos; el frontend solo
 * necesita manejar errores de red/HTTP contra ESTE servidor.
 */

export const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

/** Error normalizado y legible para mostrar en Toast / ErrorState. */
export class ApiError extends Error {
  constructor(message, { cause, status } = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.cause = cause
  }
}

async function obtenerJson(url, signal) {
  let res
  try {
    res = await fetch(url, { headers: { Accept: 'application/json' }, signal })
  } catch (err) {
    if (err?.name === 'AbortError') throw err
    throw new ApiError('No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.', {
      cause: err,
    })
  }

  if (!res.ok) {
    throw new ApiError(`El servidor respondió con un error (${res.status}). Inténtalo más tarde.`, {
      status: res.status,
    })
  }

  try {
    return await res.json()
  } catch (err) {
    throw new ApiError('La respuesta del servidor no es válida.', { cause: err })
  }
}

/**
 * Obtiene el resumen de dependencia de un país desde nuestro backend.
 * @param {string} codigoOec - id de la OEC (ej. 'sacol')
 * @param {string} anio
 * @param {AbortSignal} [signal]
 */
export function obtenerDependencia(codigoOec, anio, signal) {
  return obtenerJson(`${API_BASE}/api/paises/${codigoOec}/dependencia?anio=${anio}`, signal)
}

/**
 * Obtiene el comercio de un país con cada socio real (sin agrupar en bloques),
 * para el mapa interactivo.
 * @param {string} codigoOec - id de la OEC (ej. 'sacol')
 * @param {string} anio
 * @param {AbortSignal} [signal]
 */
export function obtenerSocios(codigoOec, anio, signal) {
  return obtenerJson(`${API_BASE}/api/paises/${codigoOec}/socios?anio=${anio}`, signal)
}

/** Obtiene el catálogo completo de países disponibles (para el buscador). */
export function obtenerCatalogo() {
  return obtenerJson(`${API_BASE}/api/paises`)
}
