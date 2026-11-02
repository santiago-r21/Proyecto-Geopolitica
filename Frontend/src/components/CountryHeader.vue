<script setup>
import CountryFlag from './CountryFlag.vue'

/** Cabecera del país: bandera, nombre, badge de monitoreo y selector (slot). */
defineProps({
  nombre: { type: String, default: '' },
  iso: { type: String, default: null },
  subtitulo: { type: String, default: '' },
})
</script>

<template>
  <div class="flex flex-col gap-4 py-5 lg:flex-row lg:items-center lg:justify-between">
    <div class="flex items-center gap-4">
      <span
        class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl border border-slate-200 bg-slate-50 shadow-xs"
      >
        <CountryFlag :iso="iso" size="w-9 h-[1.6875rem]" />
      </span>
      <div class="min-w-0">
        <div class="flex flex-wrap items-center gap-2.5">
          <h2 class="text-2xl font-bold tracking-tight text-slate-900">
            {{ nombre || '—' }}
          </h2>
          <span
            class="inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-700"
          >
            <span class="h-1.5 w-1.5 rounded-full bg-emerald-500" />
            Monitoreo activo
          </span>
        </div>
        <p class="mt-0.5 text-xs font-medium text-slate-500">{{ subtitulo }}</p>
      </div>
    </div>

    <div class="flex flex-col gap-1.5 sm:flex-row sm:items-center sm:gap-3">
      <slot />
    </div>
  </div>
</template>
