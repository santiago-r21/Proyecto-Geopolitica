<script setup>
import { computed } from 'vue'
import Card from 'primevue/card'
import ChartComponent from 'primevue/chart'
import Message from 'primevue/message'
import { ptTarjeta } from '../utils/ui.js'
import CardSkeleton from './states/CardSkeleton.vue'

const props = defineProps({
  comparacion: { type: Array, default: null },
  cargando: { type: Boolean, default: false },
  error: { type: Object, default: null },
})

const datosIndice = computed(() => ({
  labels: (props.comparacion ?? []).map((d) => d.meta.anio),
  datasets: [
    {
      label: 'Índice de dependencia',
      data: (props.comparacion ?? []).map((d) => d.indiceDependencia),
      borderColor: '#0284c7',
      backgroundColor: '#0284c7',
      tension: 0.3,
      pointRadius: 4,
    },
  ],
}))

const opcionesIndice = {
  responsive: true,
  maintainAspectRatio: false,
  scales: { y: { min: 0, max: 100, ticks: { stepSize: 20 } } },
  plugins: {
    legend: { display: false },
    tooltip: { callbacks: { label: (ctx) => ` Índice: ${ctx.parsed.y}` } },
  },
}

const datosSocios = computed(() => {
  const primero = props.comparacion?.[0]
  if (!primero) return { labels: [], datasets: [] }

  return {
    labels: props.comparacion.map((d) => d.meta.anio),
    datasets: primero.socios.map((socio) => ({
      label: socio.nombre,
      backgroundColor: socio.color,
      data: props.comparacion.map((d) => d.socios.find((s) => s.clave === socio.clave)?.porcentaje ?? 0),
    })),
  }
})

const opcionesSocios = {
  responsive: true,
  maintainAspectRatio: false,
  scales: { y: { min: 0, max: 100, ticks: { stepSize: 20 } } },
  plugins: {
    legend: { position: 'bottom', labels: { boxWidth: 10, font: { size: 11 } } },
    tooltip: { callbacks: { label: (ctx) => ` ${ctx.dataset.label}: ${ctx.parsed.y}%` } },
  },
}
</script>

<template>
  <div v-if="cargando && !comparacion">
    <CardSkeleton variante="comparacion" />
  </div>
  <Message v-else-if="error && !comparacion" severity="error" :closable="false">
    No se pudo cargar la comparación entre años: {{ error.message }}
  </Message>
  <div v-else class="space-y-6">
    <Card :pt="ptTarjeta">
      <template #content>
        <h3 class="text-base font-bold text-slate-900">Índice de dependencia por año</h3>
        <p class="mt-0.5 text-xs text-slate-500">Evolución 2022-2024 (cubo BACI de la OEC)</p>
        <div class="mt-4 h-64">
          <ChartComponent type="line" :data="datosIndice" :options="opcionesIndice" class="h-full w-full" />
        </div>
      </template>
    </Card>

    <Card :pt="ptTarjeta">
      <template #content>
        <h3 class="text-base font-bold text-slate-900">Evolución por socio</h3>
        <p class="mt-0.5 text-xs text-slate-500">Participación de cada bloque en el comercio total</p>
        <div class="mt-4 h-64">
          <ChartComponent type="bar" :data="datosSocios" :options="opcionesSocios" class="h-full w-full" />
        </div>
      </template>
    </Card>
  </div>
</template>
