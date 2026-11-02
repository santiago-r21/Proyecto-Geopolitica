<script setup>
import Toast from 'primevue/toast'
import Button from 'primevue/button'
import DashboardView from './views/DashboardView.vue'
import { useTemaStore } from './stores/tema.js'

const tema = useTemaStore()
</script>

<template>
  <div class="min-h-screen w-full bg-slate-50 text-slate-800 dark:bg-slate-950 dark:text-slate-200">
    <Button
      :icon="tema.oscuro ? 'pi pi-sun' : 'pi pi-moon'"
      text
      rounded
      severity="secondary"
      class="fixed right-3 top-3 z-30 sm:right-5 sm:top-5"
      :aria-label="tema.oscuro ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'"
      v-tooltip.left="tema.oscuro ? 'Modo claro' : 'Modo oscuro'"
      @click="tema.alternar()"
    />
    <DashboardView />
    <Toast position="top-right" />
  </div>
</template>
