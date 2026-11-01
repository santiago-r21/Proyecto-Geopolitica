<script setup>
import Card from 'primevue/card'
import PartnerDetailCard from './PartnerDetailCard.vue'
import { ptTarjeta } from '../utils/ui.js'

defineProps({
  socios: { type: Array, required: true },
})
</script>

<template>
  <Card :pt="ptTarjeta">
    <template #content>
      <div
        class="flex flex-col gap-1 border-b border-slate-100 pb-5 sm:flex-row sm:items-center sm:justify-between"
      >
        <div>
          <h3 class="text-base font-bold text-slate-900">Detalle por Socio Principal</h3>
          <p class="mt-0.5 text-xs text-slate-500">
            Desglose de áreas de influencia directa por potencia económica
          </p>
        </div>
        <span class="text-xs font-medium text-slate-400">Ordenado por cuota de influencia</span>
      </div>

      <div class="mt-6 space-y-4">
        <PartnerDetailCard v-for="socio in socios" :key="socio.clave" :socio="socio" />
      </div>
    </template>
  </Card>
</template>
