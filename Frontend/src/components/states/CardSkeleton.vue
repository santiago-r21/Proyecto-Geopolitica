<script setup>
import Skeleton from 'primevue/skeleton'

/**
 * Silueta de carga. `variante` ajusta la altura al bloque que sustituye.
 *  - indice  : tarjeta del donut
 *  - lista   : tarjeta de "Principales Socios"
 *  - rejilla : rejilla de categorías
 *  - detalle : "Detalle por Socio Principal"
 *  - mapa    : mapa interactivo de socios
 */
defineProps({
  variante: { type: String, default: 'indice' },
})
</script>

<template>
  <div v-if="variante === 'mapa'" class="rounded-2xl border border-slate-200/80 bg-white p-6 shadow-xs">
    <Skeleton width="12rem" height="1rem" class="mb-2" />
    <Skeleton width="18rem" height="0.75rem" class="mb-6" />
    <Skeleton height="26rem" />
  </div>

  <div
    v-else-if="variante === 'rejilla'"
    class="grid grid-cols-2 gap-3.5 md:grid-cols-3 lg:grid-cols-6"
  >
    <div
      v-for="n in 6"
      :key="n"
      class="rounded-xl border border-slate-200/80 bg-white p-4 shadow-xs"
    >
      <Skeleton shape="circle" size="2.25rem" class="mb-3" />
      <Skeleton width="70%" height="0.75rem" class="mb-2" />
      <Skeleton width="45%" height="1.25rem" />
    </div>
  </div>

  <div
    v-else
    class="rounded-2xl border border-slate-200/80 bg-white p-6 shadow-xs"
    :class="variante === 'indice' ? 'lg:col-span-7' : variante === 'lista' ? 'lg:col-span-5' : ''"
  >
    <Skeleton width="12rem" height="1rem" class="mb-2" />
    <Skeleton width="18rem" height="0.75rem" class="mb-6" />

    <div v-if="variante === 'indice'" class="flex flex-col items-center gap-6 sm:flex-row">
      <Skeleton shape="circle" size="11rem" />
      <div class="w-full space-y-3">
        <Skeleton v-for="n in 4" :key="n" height="1.75rem" />
      </div>
    </div>

    <div v-else class="space-y-4">
      <div v-for="n in variante === 'detalle' ? 3 : 4" :key="n">
        <Skeleton width="40%" height="0.75rem" class="mb-2" />
        <Skeleton height="0.625rem" />
      </div>
    </div>
  </div>
</template>
