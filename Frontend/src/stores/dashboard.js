import { reactive, ref, computed, watch } from 'vue'
import { defineStore } from 'pinia'
import { obtenerDependencia, obtenerSocios } from '../services/api/client.js'
import { paisPorCodigo } from '../services/oec/reference.js'

const ANIO = String(import.meta.env.VITE_OEC_YEAR ?? '2023')

/**
 * Caché por país con dedupe (una sola consulta en vuelo) y cancelación (`forzar`
 * aborta la anterior). Genérico sobre qué se consulta, para que el resumen de
 * dependencia y los socios por país (mapa) compartan la misma lógica sin
 * fragmentar el estado en stores separados.
 */
function crearRecurso(cache, cargando, error, consultar) {
  const enVuelo = {}
  const controladores = {}

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
        const datos = await consultar(pais, signal)
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

  function invalidar(codigo) {
    delete cache[codigo]
  }

  return { obtener, invalidar }
}

/**
 * Store del dashboard: qué país y pestaña están seleccionados, más los datos
 * (resumen de dependencia y socios por país) cacheados en memoria durante la
 * sesión (se pierden al recargar la página, F5 — es intencional). Cambiar
 * `codigoPais` dispara la consulta del resumen automáticamente; los socios por
 * país (más pesados, solo para el mapa) se consultan de forma perezosa, solo
 * al entrar a la pestaña "mapa".
 */
export const useDashboardStore = defineStore('dashboard', () => {
  const codigoPais = ref('CO')
  const pestanaActiva = ref('resumen')

  const cache = reactive({}) // { [codigo]: { datos, consultadoEn } }
  const cargando = reactive({}) // { [codigo]: boolean }
  const error = reactive({}) // { [codigo]: Error|null }
  const recursoDependencia = crearRecurso(cache, cargando, error, (pais, signal) =>
    obtenerDependencia(pais.oec, ANIO, signal),
  )

  const cacheSocios = reactive({}) // { [codigo]: { datos, consultadoEn } }
  const cargandoSocios = reactive({}) // { [codigo]: boolean }
  const errorSocios = reactive({}) // { [codigo]: Error|null }
  const recursoSocios = crearRecurso(cacheSocios, cargandoSocios, errorSocios, (pais, signal) =>
    obtenerSocios(pais.oec, ANIO, signal),
  )

  /** Fuerza una recarga real del resumen del país actualmente seleccionado. */
  function recargar() {
    return recursoDependencia.obtener(codigoPais.value, { forzar: true })
  }

  /** Fuerza una recarga real de los socios del país actualmente seleccionado. */
  function recargarSocios() {
    return recursoSocios.obtener(codigoPais.value, { forzar: true })
  }

  // Cambiar de país consulta el resumen automáticamente si no está en caché.
  watch(codigoPais, (codigo) => recursoDependencia.obtener(codigo), { immediate: true })

  // Los socios por país solo se consultan al entrar a la pestaña "mapa" (no al
  // cambiar de país sin abrirla) — evita una consulta pesada que casi nadie usa.
  watch(
    [codigoPais, pestanaActiva],
    ([codigo, pestana]) => {
      if (pestana === 'mapa') recursoSocios.obtener(codigo)
    },
    { immediate: true },
  )

  const datosActuales = computed(() => cache[codigoPais.value]?.datos ?? null)
  const cargandoActual = computed(() => !!cargando[codigoPais.value])
  const errorActual = computed(() => error[codigoPais.value] ?? null)

  const sociosActuales = computed(() => cacheSocios[codigoPais.value]?.datos ?? null)
  const cargandoSociosActual = computed(() => !!cargandoSocios[codigoPais.value])
  const errorSociosActual = computed(() => errorSocios[codigoPais.value] ?? null)

  return {
    codigoPais,
    pestanaActiva,
    cache,
    cargando,
    error,
    datosActuales,
    cargandoActual,
    errorActual,
    obtener: recursoDependencia.obtener,
    invalidar: recursoDependencia.invalidar,
    recargar,
    cacheSocios,
    cargandoSocios,
    errorSocios,
    sociosActuales,
    cargandoSociosActual,
    errorSociosActual,
    obtenerSocios: recursoSocios.obtener,
    invalidarSocios: recursoSocios.invalidar,
    recargarSocios,
  }
})
