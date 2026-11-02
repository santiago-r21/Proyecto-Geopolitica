import { reactive, ref, computed, watch } from 'vue'
import { defineStore } from 'pinia'
import { obtenerDependencia } from '../services/api/client.js'
import { paisPorCodigo } from '../services/oec/reference.js'

const ANIO = String(import.meta.env.VITE_OEC_YEAR ?? '2023')

/**
 * Store del dashboard: qué país y pestaña están seleccionados, más los datos de
 * dependencia por país cacheados en memoria durante la sesión (se pierden al
 * recargar la página, F5 — es intencional). Cambiar `codigoPais` dispara la
 * consulta a nuestro backend automáticamente si ese país no está ya en caché.
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
        const datos = await obtenerDependencia(pais.oec, ANIO, signal)
        if (signal.aborted) return cache[codigo]?.datos ?? null

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
