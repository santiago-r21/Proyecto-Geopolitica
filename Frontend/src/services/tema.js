/**
 * Preferencia de tema (claro/oscuro), persistida en localStorage. Mismo criterio
 * que services/favoritos.js: try/catch porque el storage puede fallar (navegación
 * privada, bloqueado) — en ese caso se degrada a "no persiste" en vez de romper la app.
 */
const CLAVE = 'geopolitica:tema'

export function cargarTema() {
  try {
    const crudo = localStorage.getItem(CLAVE)
    if (crudo === null) return true // default: oscuro
    return crudo === 'oscuro'
  } catch {
    return true
  }
}

export function guardarTema(oscuro) {
  try {
    localStorage.setItem(CLAVE, oscuro ? 'oscuro' : 'claro')
  } catch {
    // Almacenamiento no disponible — la sesión sigue funcionando solo en memoria.
  }
}
