import { computed, watch } from 'vue'
import { useDatosPaisStore } from '../stores/datosPais.js'

/**
 * Obtiene y transforma los datos de dependencia de un país desde la OEC, usando la
 * store de sesión (`datosPais`) como caché: si el país ya se consultó en esta pestaña,
 * no se vuelve a pedir a la API hasta que se llame a `recargar()` explícitamente.
 *
 * @param {import('vue').Ref<string>} codigoRef - código del país ('CO', 'PE'...)
 * @returns {{ datos, cargando, error, recargar: () => void }}
 */
export function usarDatosPais(codigoRef) {
  const store = useDatosPaisStore()

  const datos = computed(() => store.cache[codigoRef.value]?.datos ?? null)
  const cargando = computed(() => !!store.cargando[codigoRef.value])
  const error = computed(() => store.error[codigoRef.value] ?? null)

  function cargar({ forzar = false } = {}) {
    store.obtener(codigoRef.value, { forzar })
  }

  watch(codigoRef, () => cargar(), { immediate: true })

  function recargar() {
    cargar({ forzar: true })
  }

  return { datos, cargando, error, recargar }
}
