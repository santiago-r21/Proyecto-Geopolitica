import { OEC_CUBE, OEC_YEAR } from './client.js'
import {
  AREAS_SOCIO,
  CATEGORIAS,
  CLAVES_UE27,
  NIVELES,
  ORDEN_SOCIOS,
  SOCIOS,
  SUBTITULO_PAIS,
  paisPorCodigo,
  nivelPorCuota,
  nivelPorCuotaSocio,
} from './reference.js'

/*
 * Convierte las filas crudas de la OEC en el modelo que consumen los componentes.
 * TODAS las fórmulas y umbrales del dashboard viven aquí y son ajustables.
 *
 * Concepto: "dependencia" = exposición del país a las grandes potencias
 * (EE.UU., China, Unión Europea). "Otros Países" es el residual (resto del mundo).
 *
 * Qué es dato real de la OEC y qué es derivado:
 *  - socios[] / donut / % ............. REAL: cuota de cada bloque sobre el comercio
 *    total de bienes (exportaciones + importaciones) del país en el año consultado.
 *  - áreas Exportaciones / Importaciones / (Tecnología, Energía...) por socio ... REAL:
 *    cuota del socio en ese flujo o en esas secciones HS.
 *  - categorias con `secciones` ....... REAL: parte de ese dominio que depende de las
 *    tres potencias.
 *  - indiceDependencia / vulnerabilidad ... DERIVADO: índice compuesto de concentración
 *    (no es un indicador oficial).
 *  - categorías Inversión / Infraestructura y áreas marcadas «est.» ... ESTIMADO: la OEC
 *    sólo cubre comercio de bienes; se derivan del índice / de la cuota global del socio.
 */

// Pesos del índice de dependencia (deben sumar 1).
const PESOS_INDICE = { potenciaLider: 0.55, potencias: 0.3, concentracion: 0.15 }
// Curva de sensibilidad (<1 realza los valores medios-altos sin alterar el orden).
const CURVA_INDICE = 0.8
// Umbrales del índice (0-100) para la etiqueta de vulnerabilidad.
const UMBRALES_VULNERABILIDAD = { alta: 65, media: 45 }

// Los 3 bloques que se consideran "potencias" (Otros Países es residual).
const POTENCIAS = ['eeuu', 'china', 'ue']

const aNumero = (v) => (typeof v === 'number' ? v : Number(v) || 0)
const acotar = (v, min, max) => Math.min(max, Math.max(min, v))
const sumar = (obj) => Object.values(obj).reduce((s, v) => s + v, 0)
const bloquesVacios = () => ({ eeuu: 0, china: 0, ue: 0, otros: 0 })

/** Clasifica un id de país OEC en uno de los 4 bloques del dashboard. */
function bloqueDe(oecId) {
  if (oecId === SOCIOS.eeuu.oec) return 'eeuu'
  if (oecId === SOCIOS.china.oec) return 'china'
  if (CLAVES_UE27.has(oecId)) return 'ue'
  return 'otros'
}

/** Orden de presentación: potencias por cuota descendente y "Otros" siempre al final. */
function ordenarBloques(cuotaPorBloque) {
  const potencias = [...POTENCIAS].sort((a, b) => cuotaPorBloque[b] - cuotaPorBloque[a])
  return [...potencias, 'otros']
}

/** Reparte 100 puntos entre bloques con el método del mayor resto (evita 99/101%). */
function aPorcentajes(cuotaPorBloque) {
  const filas = ORDEN_SOCIOS.map((k) => {
    const exacto = (cuotaPorBloque[k] || 0) * 100
    return { k, entero: Math.floor(exacto), resto: exacto - Math.floor(exacto) }
  })
  let restantes = 100 - filas.reduce((s, f) => s + f.entero, 0)
  filas.sort((a, b) => b.resto - a.resto)
  for (let i = 0; i < filas.length && restantes > 0; i += 1, restantes -= 1) filas[i].entero += 1
  return Object.fromEntries(filas.map((f) => [f.k, Math.max(0, f.entero)]))
}

/** Suma `Trade Value` por bloque a partir de filas con un campo id concreto. */
function sumarPorBloque(filas, campoId) {
  const acc = bloquesVacios()
  for (const fila of filas) acc[bloqueDe(fila[campoId])] += aNumero(fila['Trade Value'])
  return acc
}

/** { [seccionId]: { eeuu, china, ue, otros } } combinando exportaciones + importaciones. */
function agruparPorSeccion(filasExpSeccion, filasImpSeccion) {
  const acc = {}
  const acumular = (filas, campoId) => {
    for (const fila of filas) {
      const sid = String(fila['Section ID'])
      acc[sid] ??= bloquesVacios()
      acc[sid][bloqueDe(fila[campoId])] += aNumero(fila['Trade Value'])
    }
  }
  acumular(filasExpSeccion, 'Importer Country ID')
  acumular(filasImpSeccion, 'Exporter Country ID')
  return acc
}

/** Suma los bloques de un conjunto de secciones. */
function sumarSecciones(bloquesPorSeccion, seccionesIds) {
  const totales = bloquesVacios()
  for (const sid of seccionesIds) {
    const b = bloquesPorSeccion[sid]
    if (!b) continue
    for (const k of ORDEN_SOCIOS) totales[k] += b[k]
  }
  return totales
}

/** Parte (0-1) de un dominio comercial que depende de las 3 potencias. */
function cuotaPotencias(totales) {
  const total = sumar(totales)
  if (total <= 0) return null
  return (totales.eeuu + totales.china + totales.ue) / total
}

function conNivel(cuota, { estimado = false, calcularNivel = nivelPorCuota } = {}) {
  const nivel = calcularNivel(cuota)
  return { nivel, ...NIVELES[nivel], estimado }
}

function construirCategorias({ indice, cuotaPotenciasTotal, bloquesPorSeccion, haySecciones }) {
  return CATEGORIAS.map((cat) => {
    let cuota
    let estimado = false

    if (cat.secciones === 'TODO') {
      cuota = cuotaPotenciasTotal ?? 0
    } else if (cat.secciones && haySecciones) {
      cuota = cuotaPotencias(sumarSecciones(bloquesPorSeccion, cat.secciones))
      if (cuota == null) {
        cuota = acotar((indice + (cat.offset ?? -15)) / 100, 0.05, 0.98)
        estimado = true
      }
    } else {
      // Sin fuente directa en la OEC (inversión, infraestructura) o sin datos de sección.
      cuota = acotar((indice + (cat.offset ?? -15)) / 100, 0.05, 0.98)
      estimado = true
    }

    return {
      nombre: cat.nombre,
      icono: cat.icono,
      acento: cat.acento,
      valor: Math.round(cuota * 100),
      ...conNivel(cuota, { estimado }),
    }
  })
}

function construirAreasSocio(bloque, ctx) {
  const { cuotaExportacion, cuotaImportacion, bloquesPorSeccion, haySecciones, cuotaPorBloque } = ctx

  const candidatas = AREAS_SOCIO.map((area) => {
    let cuota
    let estimado = false

    if (area.metrica === 'exportacion') {
      cuota = cuotaExportacion[bloque] ?? 0
    } else if (area.metrica === 'importacion') {
      cuota = cuotaImportacion[bloque] ?? 0
    } else if (area.metrica === 'secciones' && haySecciones) {
      const totales = sumarSecciones(bloquesPorSeccion, area.secciones)
      const total = sumar(totales)
      cuota = total > 0 ? totales[bloque] / total : 0
    } else {
      cuota = acotar((cuotaPorBloque[bloque] ?? 0) + (area.offset ?? -0.05), 0.02, 0.95)
      estimado = true
    }

    return {
      nombre: area.nombre,
      cuota,
      ...conNivel(cuota, { estimado, calcularNivel: nivelPorCuotaSocio }),
    }
  })

  const orden = { Alta: 0, Media: 1, Baja: 2 }
  candidatas.sort((a, b) => orden[a.nivel] - orden[b.nivel] || b.cuota - a.cuota)

  // 3 áreas mínimo, 5 máximo; descarta las de cuota irrelevante salvo para llegar al mínimo.
  const relevantes = candidatas.filter((a) => a.cuota >= 0.03)
  const elegidas = (relevantes.length >= 3 ? relevantes : candidatas).slice(0, 5)
  return elegidas.map(({ cuota, ...resto }) => resto)
}

/**
 * @param {[Array, Array, Array, Array]} filas - [exportacionesPorSocio, importacionesPorSocio,
 *   exportacionesPorSeccion, importacionesPorSeccion]
 * @param {string} codigo - código del país del selector (p.ej. 'CO')
 */
export function transformar(filas, codigo) {
  const [filasExp = [], filasImp = [], filasExpSeccion = [], filasImpSeccion = []] = filas
  const pais = paisPorCodigo[codigo] ?? paisPorCodigo.CO
  const haySecciones = filasExpSeccion.length > 0 || filasImpSeccion.length > 0

  const expPorBloque = sumarPorBloque(filasExp, 'Importer Country ID')
  const impPorBloque = sumarPorBloque(filasImp, 'Exporter Country ID')
  const exportacionTotal = sumar(expPorBloque)
  const importacionTotal = sumar(impPorBloque)

  const combinado = bloquesVacios()
  for (const k of ORDEN_SOCIOS) combinado[k] = expPorBloque[k] + impPorBloque[k]
  const comercioTotal = sumar(combinado)

  const cuotaPorBloque = bloquesVacios()
  const cuotaExportacion = bloquesVacios()
  const cuotaImportacion = bloquesVacios()
  for (const k of ORDEN_SOCIOS) {
    cuotaPorBloque[k] = comercioTotal > 0 ? combinado[k] / comercioTotal : 0
    cuotaExportacion[k] = exportacionTotal > 0 ? expPorBloque[k] / exportacionTotal : 0
    cuotaImportacion[k] = importacionTotal > 0 ? impPorBloque[k] / importacionTotal : 0
  }

  // --- Índice de dependencia (derivado) ---
  const potenciaLider = Math.max(...POTENCIAS.map((k) => cuotaPorBloque[k]))
  const sumaPotencias = POTENCIAS.reduce((s, k) => s + cuotaPorBloque[k], 0)
  // Concentración ENTRE las potencias (HHI renormalizado, 0.33 = reparto equitativo, 1 = una sola).
  const hhiPotencias =
    sumaPotencias > 0
      ? POTENCIAS.reduce((s, k) => s + (cuotaPorBloque[k] / sumaPotencias) ** 2, 0)
      : 0
  const indiceBruto =
    PESOS_INDICE.potenciaLider * potenciaLider +
    PESOS_INDICE.potencias * sumaPotencias +
    PESOS_INDICE.concentracion * hhiPotencias
  const indiceDependencia = acotar(Math.round(100 * indiceBruto ** CURVA_INDICE), 0, 100)

  const porcentajes = aPorcentajes(cuotaPorBloque)
  const orden = ordenarBloques(cuotaPorBloque)
  const socios = orden.map((k) => ({ ...SOCIOS[k], porcentaje: porcentajes[k] }))

  const bloquesPorSeccion = agruparPorSeccion(filasExpSeccion, filasImpSeccion)
  const cuotaPotenciasTotal = comercioTotal > 0 ? sumaPotencias : 0

  const categorias = construirCategorias({
    indice: indiceDependencia,
    cuotaPotenciasTotal,
    bloquesPorSeccion,
    haySecciones,
  })

  const ctxAreas = {
    cuotaExportacion,
    cuotaImportacion,
    bloquesPorSeccion,
    haySecciones,
    cuotaPorBloque,
  }
  const detalleSocios = orden.map((k) => ({
    ...SOCIOS[k],
    porcentaje: porcentajes[k],
    areas: construirAreasSocio(k, ctxAreas),
  }))

  return {
    codigo: pais.codigo,
    nombre: pais.nombre,
    iso: pais.iso,
    subtitulo: SUBTITULO_PAIS,
    indiceDependencia,
    vulnerabilidad: vulnerabilidadPara(indiceDependencia),
    pesos: { economico: 50, estrategico: 50 },
    socios,
    categorias,
    detalleSocios,
    meta: {
      fuente: 'OEC',
      dataset: 'BACI · HS-2022',
      cubo: OEC_CUBE,
      anio: OEC_YEAR,
      haySecciones,
      consultadoEn: new Date().toISOString(),
    },
  }
}

function vulnerabilidadPara(indice) {
  if (indice >= UMBRALES_VULNERABILIDAD.alta) {
    return { etiqueta: 'Alta Vulnerabilidad', severity: 'danger' }
  }
  if (indice >= UMBRALES_VULNERABILIDAD.media) {
    return { etiqueta: 'Vulnerabilidad Media', severity: 'warn' }
  }
  return { etiqueta: 'Vulnerabilidad Baja', severity: 'success' }
}
