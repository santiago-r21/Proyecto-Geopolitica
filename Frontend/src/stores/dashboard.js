import { reactive, ref, computed, watch } from 'vue'
import { defineStore } from 'pinia'
import { obtenerDependencia, obtenerCatalogo } from '../services/api/client.js'
import { cargarFavoritos, guardarFavoritos } from '../services/favoritos.js'
import { FAVORITOS_POR_DEFECTO } from '../services/oec/reference.js'

const ANIO = String(import.meta.env.VITE_OEC_YEAR ?? '2023')

/**
 * Store del dashboard: qué país y pestaña están seleccionados, el catálogo
 * completo de países (para el buscador) y los datos de dependencia por país
 * cacheados en memoria durante la sesión (se pierden al recargar la página,
 * F5 — es intencional). Cambiar `codigoPais` dispara la consulta a nuestro
 * backend automáticamente si ese país no está ya en caché.
 */
export const useDashboardStore = defineStore('dashboard', () => {
  const codigoPais = ref('sacol') // id de la OEC (ej. 'sacol' = Colombia)
  const pestanaActiva = ref('resumen')

  const cache = reactive({}) // { [codigo]: { datos, consultadoEn } }
  const cargando = reactive({}) // { [codigo]: boolean }
  const error = reactive({}) // { [codigo]: Error|null }

  // No reactivos a propósito: son detalles de implementación (dedupe/cancelación),
  // no estado que la UI necesite observar.
  const enVuelo = {}
  const controladores = {}

  // Catálogo completo de países (GET /api/paises), para el buscador. Se carga
  // una sola vez, no por país: la validación de "país soportado" ya la hace
  // el backend, así que el store no necesita una lista propia para eso.
  const catalogo = ref([])
  const cargandoCatalogo = ref(false)
  const catalogoError = ref(null)
  const catalogoPorCodigo = computed(() => Object.fromEntries(catalogo.value.map((p) => [p.codigo, p])))

  async function cargarCatalogo() {
    if (catalogo.value.length || cargandoCatalogo.value) return
    cargandoCatalogo.value = true
    catalogoError.value = null
    try {
      catalogo.value = await obtenerCatalogo()
    } catch (err) {
      catalogoError.value = err
    } finally {
      cargandoCatalogo.value = false
    }
  }

  /**
   * Resuelve los datos de un país, usando caché si ya existen.
   * @param {string} codigo - id de la OEC (ej. 'sacol')
   * @param {{ forzar?: boolean }} [opciones] - `forzar: true` ignora la caché y recarga.
   */
  async function obtener(codigo, { forzar = false } = {}) {
    if (!forzar && cache[codigo]) return cache[codigo].datos
    if (!forzar && enVuelo[codigo]) return enVuelo[codigo]

    if (forzar) controladores[codigo]?.abort()

    const controller = new AbortController()
    controladores[codigo] = controller
    const { signal } = controller

    cargando[codigo] = true
    error[codigo] = null

    const promesa = (async () => {
      try {
        const datos = await obtenerDependencia(codigo, ANIO, signal)
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

  // Favoritos del usuario (localStorage, sin backend — ver services/favoritos.js).
  const favoritos = ref(cargarFavoritos(FAVORITOS_POR_DEFECTO))

  function esFavorito(codigo) {
    return favoritos.value.includes(codigo)
  }

  function alternarFavorito(codigo) {
    favoritos.value = esFavorito(codigo)
      ? favoritos.value.filter((c) => c !== codigo)
      : [...favoritos.value, codigo]
    guardarFavoritos(favoritos.value)
  }

  cargarCatalogo()

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
    catalogo,
    cargandoCatalogo,
    catalogoError,
    catalogoPorCodigo,
    cargarCatalogo,
    favoritos,
    esFavorito,
    alternarFavorito,
  }
})
