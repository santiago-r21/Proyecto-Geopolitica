import { reactive, ref, computed, watch } from 'vue'
import { defineStore } from 'pinia'
import { fetchRecords } from '../services/oec/client.js'
import {
  exportacionesPorSocio,
  exportacionesPorSeccion,
  importacionesPorSocio,
  importacionesPorSeccion,
} from '../services/oec/queries.js'
import { paisPorCodigo } from '../services/oec/reference.js'
import { transformar } from '../services/oec/transform.js'

/**
 * Store del dashboard: qué país y pestaña están seleccionados, más los datos de
 * dependencia por país cacheados en memoria durante la sesión (se pierden al
 * recargar la página, F5 — es intencional). Cambiar `codigoPais` dispara la
 * consulta a la OEC automáticamente si ese país no está ya en caché.
 */
export const useDashboardStore = defineStore('dashboard', () => {
  const codigoPais = ref('CO')
  const pestanaActiva = ref('resumen')

  const cache = reactive({}) // { [codigo]: { datos, consultadoEn } }
  const cargando = reactive({}) // { [codigo]: boolean }
  const error = reactive({}) // { [codigo]: Error|null }

  // No reactivos a propósito: son detalles de implementación (dedupe/cancelación),
  // no estado que la UI necesite observar.
  const enVuelo = {}
  const controladores = {}

  /**
   * Resuelve los datos de un país, usando caché si ya existen.
   * @param {string} codigo
   * @param {{ forzar?: boolean }} [opciones] - `forzar: true` ignora la caché y recarga.
   */
  async function obtener(codigo, { forzar = false } = {}) {
    if (!forzar && cache[codigo]) return cache[codigo].datos
    if (!forzar && enVuelo[codigo]) return enVuelo[codigo]

    if (forzar) controladores[codigo]?.abort()

    const pais = paisPorCodigo[codigo]
    if (!pais) {
      error[codigo] = new Error(`País no soportado: ${codigo}`)
      return null
    }

    const controller = new AbortController()
    controladores[codigo] = controller
    const { signal } = controller

    cargando[codigo] = true
    error[codigo] = null

    const promesa = (async () => {
      try {
        // Consultas obligatorias: potencias por exportación e importación.
        const [filasExp, filasImp] = await Promise.all([
          fetchRecords(exportacionesPorSocio(pais.oec), signal),
          fetchRecords(importacionesPorSocio(pais.oec), signal),
        ])

        // Consultas best-effort: desglose por sección HS (para categorías reales).
        const [secExp, secImp] = await Promise.allSettled([
          fetchRecords(exportacionesPorSeccion(pais.oec), signal),
          fetchRecords(importacionesPorSeccion(pais.oec), signal),
        ])

        if (signal.aborted) return cache[codigo]?.datos ?? null

        const datos = transformar(
          [
            filasExp,
            filasImp,
            secExp.status === 'fulfilled' ? secExp.value : [],
            secImp.status === 'fulfilled' ? secImp.value : [],
          ],
          codigo,
        )

        cache[codigo] = { datos, consultadoEn: Date.now() }
        return datos
      } catch (err) {
        if (err?.name === 'AbortError' || signal.aborted) return cache[codigo]?.datos ?? null
        // No cacheamos el error: así el próximo intento (cambio de país o "recargar")
        // vuelve a consultar en vez de quedar atascado.
        error[codigo] = err
        return cache[codigo]?.datos ?? null
      } finally {
        if (!signal.aborted) cargando[codigo] = false
        delete enVuelo[codigo]
      }
    })()

    enVuelo[codigo] = promesa
    return promesa
  }

  /** Descarta la caché de un país (fuerza una consulta real en el próximo `obtener`). */
  function invalidar(codigo) {
    delete cache[codigo]
  }

  /** Fuerza una recarga real del país actualmente seleccionado. */
  function recargar() {
    return obtener(codigoPais.value, { forzar: true })
  }

  // Cambiar de país consulta sus datos automáticamente si no están en caché.
  watch(codigoPais, (codigo) => obtener(codigo), { immediate: true })

  const datosActuales = computed(() => cache[codigoPais.value]?.datos ?? null)
  const cargandoActual = computed(() => !!cargando[codigoPais.value])
  const errorActual = computed(() => error[codigoPais.value] ?? null)

  return {
    codigoPais,
    pestanaActiva,
    cache,
    cargando,
    error,
    datosActuales,
    cargandoActual,
    errorActual,
    obtener,
    invalidar,
    recargar,
  }
})
