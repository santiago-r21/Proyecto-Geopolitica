import { ref } from 'vue'
import { defineStore } from 'pinia'
import { cargarTema, guardarTema } from '../services/tema.js'

/**
 * Tema claro/oscuro del sitio. La clase `app-dark` en <html> es la única fuente
 * de verdad: PrimeVue la usa como `darkModeSelector` y Tailwind la usa para la
 * variante `dark:` (ver `@custom-variant dark` en style.css) — un solo toggle
 * mueve ambos sistemas de estilos a la vez.
 */
export const useTemaStore = defineStore('tema', () => {
  const oscuro = ref(cargarTema())

  function aplicar() {
    document.documentElement.classList.toggle('app-dark', oscuro.value)
  }

  function alternar() {
    oscuro.value = !oscuro.value
    guardarTema(oscuro.value)
    aplicar()
  }

  aplicar()

  return { oscuro, alternar }
})
