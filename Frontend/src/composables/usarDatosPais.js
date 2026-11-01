import { ref, shallowRef, watch } from 'vue'
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
 * Obtiene y transforma en vivo los datos de dependencia de un país desde la OEC.
 * Sin persistencia: cada cambio de `codigoRef` dispara una nueva consulta.
 * El último resultado se mantiene en memoria sólo para no parpadear durante la recarga.
 *
 * @param {import('vue').Ref<string>} codigoRef - código del país ('CO', 'PE'...)
 * @returns {{ datos, cargando, error, recargar: () => void }}
 */
export function usarDatosPais(codigoRef) {
  const datos = shallowRef(null)
  const cargando = ref(false)
  const error = ref(null)

  let controller = null

  async function cargar() {
    const codigo = codigoRef.value
    const pais = paisPorCodigo[codigo]
    if (!pais) {
      error.value = new Error(`País no soportado: ${codigo}`)
      return
    }

    controller?.abort()
    controller = new AbortController()
    const { signal } = controller

    cargando.value = true
    error.value = null

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

      if (signal.aborted) return

      datos.value = transformar(
        [
          filasExp,
          filasImp,
          secExp.status === 'fulfilled' ? secExp.value : [],
          secImp.status === 'fulfilled' ? secImp.value : [],
        ],
        codigo,
      )
    } catch (err) {
      if (err?.name === 'AbortError' || signal.aborted) return
      error.value = err
    } finally {
      if (!signal.aborted) cargando.value = false
    }
  }

  watch(codigoRef, cargar, { immediate: true })

  return { datos, cargando, error, recargar: cargar }
}
