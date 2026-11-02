/**
 * Países disponibles en el selector (accesos rápidos).
 * `oec` = id de la OEC (prefijo continente + ISO3), usado para consultar el backend;
 * `iso` = código de flag-icons.
 */
export const PAISES = [
  { codigo: 'CO', nombre: 'Colombia', iso: 'co', oec: 'sacol' },
  { codigo: 'PE', nombre: 'Perú', iso: 'pe', oec: 'saper' },
  { codigo: 'CL', nombre: 'Chile', iso: 'cl', oec: 'sachl' },
  { codigo: 'MX', nombre: 'México', iso: 'mx', oec: 'namex' },
  { codigo: 'AR', nombre: 'Argentina', iso: 'ar', oec: 'saarg' },
]

export const paisPorCodigo = Object.fromEntries(PAISES.map((p) => [p.codigo, p]))
