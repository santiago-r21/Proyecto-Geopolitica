<script setup>
import Toast from 'primevue/toast'
import Button from 'primevue/button'
import DashboardView from './views/DashboardView.vue'
import { useTemaStore } from './stores/tema.js'

/**
 * Raíz de la app: el dashboard completo + dos piezas globales que no
 * pertenecen a ninguna pestaña en particular — el Toast (avisos de
 * carga/error) y el botón flotante de tema claro/oscuro.
 */
const tema = useTemaStore()
</script>

<template>
  <div class="min-h-screen w-full bg-slate-50 text-slate-800 dark:bg-slate-950 dark:text-slate-200">
    <Button
      :icon="tema.oscuro ? 'pi pi-sun' : 'pi pi-moon'"
      rounded
      severity="secondary"
      class="fixed bottom-3 right-3 z-30 h-11 w-11 shadow-lg sm:bottom-5 sm:right-10"
      :aria-label="tema.oscuro ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'"
      v-tooltip.top="tema.oscuro ? 'Modo claro' : 'Modo oscuro'"
      @click="tema.alternar()"
    />
    <DashboardView />
    <Toast position="top-right" />
  </div>
</template>
