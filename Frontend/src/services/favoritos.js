/**
 * Favoritos del usuario, persistidos en localStorage (sin backend, sin cuentas:
 * cada navegador arma su propia lista). Envuelto en try/catch porque el storage
 * puede fallar (navegación privada, bloqueado) — en ese caso se degrada a
 * "no persiste" en vez de romper la app.
 */
const CLAVE = 'geopolitica:favoritos'

export function cargarFavoritos(porDefecto) {
  try {
    const crudo = localStorage.getItem(CLAVE)
    if (!crudo) return [...porDefecto]
    const lista = JSON.parse(crudo)
    return Array.isArray(lista) ? lista : [...porDefecto]
  } catch {
    return [...porDefecto]
  }
}

export function guardarFavoritos(lista) {
  try {
    localStorage.setItem(CLAVE, JSON.stringify(lista))
  } catch {
    // Almacenamiento no disponible — la sesión sigue funcionando solo en memoria.
  }
}
