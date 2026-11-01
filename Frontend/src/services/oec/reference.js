/**
 * Metadatos estáticos para consumir la OEC API y mapearla al modelo del dashboard.
 * Cambiar de fuente de datos o añadir países se resuelve tocando SÓLO este archivo
 * y `transform.js`; los componentes no dependen de la OEC.
 */

/**
 * Países disponibles en el selector.
 * `oec` = id de la OEC (prefijo continente + ISO3); `iso` = código de flag-icons.
 */
export const PAISES = [
  { codigo: 'CO', nombre: 'Colombia', iso: 'co', oec: 'sacol' },
  { codigo: 'PE', nombre: 'Perú', iso: 'pe', oec: 'saper' },
  { codigo: 'CL', nombre: 'Chile', iso: 'cl', oec: 'sachl' },
  { codigo: 'MX', nombre: 'México', iso: 'mx', oec: 'namex' },
  { codigo: 'AR', nombre: 'Argentina', iso: 'ar', oec: 'saarg' },
]

export const paisPorCodigo = Object.fromEntries(PAISES.map((p) => [p.codigo, p]))

/** Texto secundario del encabezado (igual para todos los países). */
export const SUBTITULO_PAIS = 'Dependencia económica y estratégica'

/**
 * Los 4 bloques (potencias) que agrupa el dashboard.
 * `color` se usa también como `backgroundColor` del donut (Chart.js).
 */
export const SOCIOS = {
  eeuu: { clave: 'eeuu', nombre: 'Estados Unidos', iso: 'us', color: '#0284c7', oec: 'nausa' },
  china: { clave: 'china', nombre: 'China', iso: 'cn', color: '#e11d48', oec: 'aschn' },
  ue: { clave: 'ue', nombre: 'Unión Europea', iso: 'eu', color: '#10b981' },
  otros: { clave: 'otros', nombre: 'Otros Países', iso: null, color: '#8b5cf6' },
}

export const ORDEN_SOCIOS = ['eeuu', 'china', 'ue', 'otros']

/**
 * Claves OEC de los 27 estados miembro de la UE (prefijo `eu` + ISO3).
 * Ojo: en la OEC el prefijo `eu` significa "Europa" (incluye Reino Unido, Rusia,
 * Suiza, Noruega...), por eso hace falta esta lista explícita para aislar la UE-27.
 */
export const CLAVES_UE27 = new Set([
  'euaut', 'eubel', 'eubgr', 'euhrv', 'eucyp', 'eucze', 'eudnk', 'euest', 'eufin',
  'eufra', 'eudeu', 'eugrc', 'euhun', 'euirl', 'euita', 'eulva', 'eultu', 'eulux',
  'eumlt', 'eunld', 'eupol', 'euprt', 'eurou', 'eusvk', 'eusvn', 'euesp', 'euswe',
])

/** Niveles de dependencia → severidad de PrimeVue Tag + color de borde Tailwind. */
export const NIVELES = {
  Alta: { etiqueta: 'Alta', severity: 'danger', borde: 'border-rose-200' },
  Media: { etiqueta: 'Media', severity: 'warn', borde: 'border-amber-200' },
  Baja: { etiqueta: 'Baja', severity: 'success', borde: 'border-emerald-200' },
}

/** Umbrales (0-1) para un dominio completo: parte que depende de las 3 potencias. */
export function nivelPorCuota(cuota) {
  if (cuota >= 0.6) return 'Alta'
  if (cuota >= 0.35) return 'Media'
  return 'Baja'
}

/** Umbrales (0-1) para la cuota de UN socio individual (más exigentes). */
export function nivelPorCuotaSocio(cuota) {
  if (cuota >= 0.45) return 'Alta'
  if (cuota >= 0.22) return 'Media'
  return 'Baja'
}

/**
 * Categorías del bloque "Dependencia por categoría".
 * `secciones`: ids de Sección HS (nivel `Section` de la OEC, 1..21) con los que se
 * calcula la concentración real. `'TODO'` = todo el comercio. `null` = estimado
 * (la OEC sólo cubre comercio de bienes, no inversión ni infraestructura).
 */
export const CATEGORIAS = [
  { nombre: 'Comercio', icono: 'pi pi-shopping-cart', acento: 'sky', secciones: 'TODO' },
  { nombre: 'Inversión', icono: 'pi pi-dollar', acento: 'emerald', secciones: null, offset: -19 },
  { nombre: 'Tecnología', icono: 'pi pi-microchip', acento: 'indigo', secciones: ['16', '18'] },
  { nombre: 'Energía', icono: 'pi pi-bolt', acento: 'amber', secciones: ['5'] },
  { nombre: 'Infraestructura', icono: 'pi pi-building', acento: 'purple', secciones: null, offset: -27 },
  { nombre: 'Recursos estratégicos', icono: 'pi pi-sparkles', acento: 'teal', secciones: ['14', '15'] },
]

/**
 * Áreas de influencia mostradas por cada socio en "Detalle por Socio Principal".
 * `metrica`: cómo se calcula el nivel.
 *   - exportacion/importacion: cuota real del socio en las exportaciones/importaciones totales.
 *   - secciones: cuota real del socio dentro de esas secciones HS.
 *   - estimado: derivado de la cuota global del socio + offset (marcado como estimado).
 */
export const AREAS_SOCIO = [
  { nombre: 'Exportaciones', metrica: 'exportacion' },
  { nombre: 'Importaciones', metrica: 'importacion' },
  { nombre: 'Tecnología', metrica: 'secciones', secciones: ['16', '18'] },
  { nombre: 'Inversiones', metrica: 'estimado', offset: -0.05 },
  { nombre: 'Energía', metrica: 'secciones', secciones: ['5'] },
  { nombre: 'Infraestructura', metrica: 'estimado', offset: -0.12 },
  { nombre: 'Recursos estratégicos', metrica: 'secciones', secciones: ['14', '15'] },
]
