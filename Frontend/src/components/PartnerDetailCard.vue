<script setup>
import ProgressBar from 'primevue/progressbar'
import Tag from 'primevue/tag'
import CountryFlag from './CountryFlag.vue'
import { AYUDA_ESTIMADO } from '../utils/ui.js'

defineProps({
  socio: { type: Object, required: true },
})
</script>

<template>
  <article
    class="rounded-xl border border-slate-200/60 bg-slate-50/70 p-4 transition-colors hover:border-slate-300 dark:border-slate-800 dark:bg-slate-800/40 dark:hover:border-slate-700"
  >
    <div class="mb-3 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
      <div class="flex items-center gap-2.5">
        <CountryFlag :iso="socio.iso" size="w-6 h-[1.125rem]" />
        <h4 class="text-sm font-bold text-slate-900 dark:text-slate-50">{{ socio.nombre }}</h4>
      </div>
      <div class="flex w-full items-center gap-3 sm:w-1/2 sm:max-w-xs">
        <ProgressBar
          :value="socio.porcentaje"
          :show-value="false"
          class="flex-1"
          :pt="{
            root: 'h-2 overflow-hidden rounded-full bg-slate-200 dark:bg-slate-700',
            value: { class: 'rounded-full', style: { backgroundColor: socio.color } },
          }"
        />
        <span class="shrink-0 text-xs font-extrabold text-slate-800 dark:text-slate-100">{{ socio.porcentaje }}%</span>
      </div>
    </div>

    <div class="flex flex-wrap gap-2 pt-1">
      <span
        v-for="area in socio.areas"
        :key="area.nombre"
        v-tooltip.top="area.estimado ? AYUDA_ESTIMADO : ''"
        class="inline-flex items-center gap-1.5 rounded-md border bg-white px-2.5 py-1 text-xs font-semibold text-slate-700 shadow-2xs dark:bg-slate-900 dark:text-slate-300"
        :class="area.borde"
      >
        {{ area.nombre }}
        <span
          v-if="area.estimado"
          class="text-[9px] font-bold uppercase text-slate-400 dark:text-slate-500"
          aria-hidden="true"
        >
          est.
        </span>
        <Tag
          :value="area.etiqueta"
          :severity="area.severity"
          :pt="{ root: 'rounded px-1 py-0 text-[10px] font-bold uppercase leading-4' }"
        />
      </span>
    </div>
  </article>
</template>
