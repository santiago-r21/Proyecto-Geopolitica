/**
 * Cliente HTTP mínimo para la OEC API (motor Tesseract).
 * La API responde con `Access-Control-Allow-Origin: *`, así que se llama
 * directamente desde el navegador (sin proxy) tanto en dev como en producción.
 */

export const OEC_BASE = import.meta.env.VITE_OEC_BASE ?? 'https://api-v2.oec.world/tesseract'
export const OEC_CUBE = import.meta.env.VITE_OEC_CUBE ?? 'trade_i_baci_a_22'
export const OEC_YEAR = String(import.meta.env.VITE_OEC_YEAR ?? '2023')

/** Error normalizado y legible para mostrar en Toast / ErrorState. */
export class OecError extends Error {
  constructor(message, { cause, status } = {}) {
    super(message)
    this.name = 'OecError'
    this.status = status
    this.cause = cause
  }
}

const REINTENTOS = 2
const ESPERA_MS = 1500

/** Pausa cancelable: rechaza con AbortError si la petición se aborta. */
function esperar(ms, signal) {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) return reject(new DOMException('Aborted', 'AbortError'))
    const id = setTimeout(resolve, ms)
    signal?.addEventListener(
      'abort',
      () => {
        clearTimeout(id)
        reject(new DOMException('Aborted', 'AbortError'))
      },
      { once: true },
    )
  })
}

/**
 * Ejecuta una consulta `data.jsonrecords` y devuelve el array `data`.
 * @param {string} path - ruta relativa, p.ej. `data.jsonrecords?cube=...`
 * @param {AbortSignal} [signal]
 * @returns {Promise<Array<Record<string, unknown>>>}
 */
export async function fetchRecords(path, signal) {
  const url = `${OEC_BASE}/${path}`
  let res
  // La OEC devuelve 5xx esporádicos cuando la consulta aún no está en su caché;
  // el mismo request suele funcionar al reintentar.
  for (let intento = 0; intento <= REINTENTOS; intento++) {
    if (intento > 0) await esperar(ESPERA_MS * intento, signal)
    try {
      res = await fetch(url, { headers: { Accept: 'application/json' }, signal })
    } catch (err) {
      if (err?.name === 'AbortError') throw err
      throw new OecError('No se pudo conectar con la OEC API. Revisa tu conexión e inténtalo de nuevo.', {
        cause: err,
      })
    }
    if (res.status < 500) break
  }

  if (!res.ok) {
    throw new OecError(`La OEC API respondió con un error (${res.status}). Inténtalo más tarde.`, {
      status: res.status,
    })
  }

  let body
  try {
    body = await res.json()
  } catch (err) {
    throw new OecError('La respuesta de la OEC API no es válida.', { cause: err })
  }

  if (!Array.isArray(body?.data)) {
    throw new OecError('La OEC API no devolvió datos para la consulta.')
  }
  return body.data
}
