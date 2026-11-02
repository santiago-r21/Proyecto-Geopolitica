<script setup>
import Card from 'primevue/card'
import ProgressBar from 'primevue/progressbar'
import CountryFlag from './CountryFlag.vue'
import { ptTarjeta } from '../utils/ui.js'

defineProps({
  socios: { type: Array, required: true },
})
</script>

<template>
  <Card :pt="ptTarjeta">
    <template #content>
      <div class="flex flex-wrap items-center justify-between gap-x-3 gap-y-1">
        <h3 class="text-base font-bold text-slate-900 dark:text-slate-50">Principales Socios</h3>
        <span class="text-xs font-medium text-slate-400 dark:text-slate-500">Participación global</span>
      </div>

      <ul class="mt-5 space-y-4">
        <li v-for="socio in socios" :key="socio.clave">
          <div class="mb-1.5 flex items-center justify-between text-xs">
            <span class="flex items-center gap-2">
              <CountryFlag :iso="socio.iso" size="w-4 h-3" />
              <span class="font-semibold text-slate-800 dark:text-slate-200">{{ socio.nombre }}</span>
            </span>
            <span class="font-bold text-slate-900 dark:text-slate-50">{{ socio.porcentaje }}%</span>
          </div>
          <ProgressBar
            :value="socio.porcentaje"
            :show-value="false"
            v-tooltip.top="`${socio.nombre}: ${socio.porcentaje}% del comercio total`"
            :pt="{
              root: 'h-2.5 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800',
              value: { class: 'rounded-full', style: { backgroundColor: socio.color } },
            }"
          />
        </li>
      </ul>

      <div
        class="mt-5 flex items-center justify-between border-t border-slate-100 pt-3 text-[11px] text-slate-400 dark:border-slate-800 dark:text-slate-500"
      >
        <span>Exportaciones + importaciones de bienes</span>
        <span class="font-semibold text-slate-600 dark:text-slate-300">Total: 100%</span>
      </div>
    </template>
  </Card>
</template>
