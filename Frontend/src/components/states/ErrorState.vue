<script setup>
import Button from 'primevue/button'

defineProps({
  mensaje: { type: String, default: 'No se pudieron cargar los datos.' },
})
defineEmits(['reintentar'])
</script>

<template>
  <div
    class="flex flex-col items-center justify-center gap-3 rounded-2xl border border-rose-200 bg-rose-50/60 p-10 text-center dark:border-rose-900 dark:bg-rose-500/10"
  >
    <span class="flex h-12 w-12 items-center justify-center rounded-xl bg-rose-100 text-rose-600 dark:bg-rose-500/15 dark:text-rose-400">
      <i class="pi pi-exclamation-triangle text-xl" />
    </span>
    <h3 class="text-base font-bold text-slate-900 dark:text-slate-50">Error al consultar el servidor</h3>
    <p class="max-w-md text-sm text-slate-500 dark:text-slate-400">{{ mensaje }}</p>
    <Button
      label="Reintentar"
      icon="pi pi-refresh"
      size="small"
      severity="danger"
      outlined
      @click="$emit('reintentar')"
    />
  </div>
</template>
