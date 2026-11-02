<script setup>
import Button from 'primevue/button'

defineProps({
  mensaje: { type: String, default: 'No se pudieron cargar los datos.' },
})
defineEmits(['reintentar'])
</script>

<template>
  <div
    class="flex flex-col items-center justify-center gap-3 rounded-2xl border border-rose-200 bg-rose-50/60 p-10 text-center"
  >
    <span class="flex h-12 w-12 items-center justify-center rounded-xl bg-rose-100 text-rose-600">
      <i class="pi pi-exclamation-triangle text-xl" />
    </span>
    <h3 class="text-base font-bold text-slate-900">Error al consultar el servidor</h3>
    <p class="max-w-md text-sm text-slate-500">{{ mensaje }}</p>
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
