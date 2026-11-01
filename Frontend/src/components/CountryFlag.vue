<script setup>
/**
 * Bandera multiplataforma con `flag-icons` (Windows no renderiza emoji de banderas).
 * `iso` nulo/vacío → icono de globo (usado para "Otros Países").
 */
defineProps({
  iso: { type: String, default: null },
  size: { type: String, default: 'w-6 h-[1.125rem]' },
})
</script>

<template>
  <span
    v-if="iso"
    class="fi inline-block shrink-0 rounded-[3px] bg-cover bg-center shadow-2xs ring-1 ring-black/10"
    :class="[`fi-${iso}`, size]"
  />
  <span
    v-else
    class="inline-flex shrink-0 items-center justify-center rounded-[3px] bg-violet-50 text-violet-500"
    :class="size"
  >
    <i class="pi pi-globe text-xs" />
  </span>
</template>
