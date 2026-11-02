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

// Colores por "acento" de categoría, mismas familias que utils/ui.js (clasesAcento).
const COLOR_ACENTO = {
  sky: '#0284c7',
  emerald: '#10b981',
  indigo: '#6366f1',
  amber: '#f59e0b',
  purple: '#a855f7',
  teal: '#14b8a6',
  rose: '#f43f5e',
}

// El índice compuesto se mueve poco de un año a otro; las categorías
// individuales varían más y muestran mejor el cambio en el tiempo.
const datosCategorias = computed(() => {
  const primero = props.comparacion?.[0]
  if (!primero) return { labels: [], datasets: [] }

  return {
    labels: props.comparacion.map((d) => d.meta.anio),
    datasets: primero.categorias.map((categoria) => ({
      label: categoria.nombre,
      borderColor: COLOR_ACENTO[categoria.acento] ?? '#64748b',
      backgroundColor: COLOR_ACENTO[categoria.acento] ?? '#64748b',
      tension: 0.3,
      pointRadius: 4,
      data: props.comparacion.map(
        (d) => d.categorias.find((c) => c.nombre === categoria.nombre)?.valor ?? 0,
      ),
    })),
  }
})

const opcionesCategorias = {
  responsive: true,
  maintainAspectRatio: false,
  scales: { y: { min: 0, max: 100, ticks: { stepSize: 20 } } },
  plugins: {
    legend: { position: 'bottom', labels: { boxWidth: 10, font: { size: 11 } } },
    tooltip: { callbacks: { label: (ctx) => ` ${ctx.dataset.label}: ${ctx.parsed.y}%` } },
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
        <h3 class="text-base font-bold text-slate-900">Dependencia por categoría</h3>
        <p class="mt-0.5 text-xs text-slate-500">Evolución 2022-2024 por área (comercio, tecnología, energía...)</p>
        <div class="mt-4 h-64">
          <ChartComponent type="line" :data="datosCategorias" :options="opcionesCategorias" class="h-full w-full" />
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
