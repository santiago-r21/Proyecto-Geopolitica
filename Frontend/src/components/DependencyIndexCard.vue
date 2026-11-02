<script setup>
import { computed } from 'vue'
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import Chart from 'primevue/chart'
import { ptTarjeta } from '../utils/ui.js'
import { useTemaStore } from '../stores/tema.js'

const props = defineProps({
  indice: { type: Number, required: true },
  vulnerabilidad: { type: Object, required: true },
  socios: { type: Array, required: true },
  pesos: { type: Object, default: () => ({ economico: 50, estrategico: 50 }) },
})

const tema = useTemaStore()

const datosGrafico = computed(() => ({
  labels: props.socios.map((s) => s.nombre),
  datasets: [
    {
      data: props.socios.map((s) => s.porcentaje),
      backgroundColor: props.socios.map((s) => s.color),
      hoverBackgroundColor: props.socios.map((s) => s.color),
      // Mismo color que el fondo de la tarjeta (ptTarjeta): el borde separa
      // segmentos sin quedar "quemado" en blanco sobre fondo oscuro.
      borderColor: tema.oscuro ? '#0f172a' : '#ffffff',
      borderWidth: 2,
      hoverOffset: 6,
    },
  ],
}))

const opcionesGrafico = computed(() => ({
  cutout: '70%',
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: { label: (ctx) => ` ${ctx.label}: ${ctx.parsed}%` },
    },
  },
}))
</script>

<template>
  <Card :pt="ptTarjeta">
    <template #content>
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0">
          <h3 class="text-base font-bold text-slate-900 dark:text-slate-50">Índice dependencia internacional</h3>
          <p class="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
            Nivel agregado de exposición a potencias externas
          </p>
        </div>
        <Tag
          v-tooltip.left="'Clasificación derivada del índice compuesto de concentración comercial.'"
          :value="vulnerabilidad.etiqueta"
          :severity="vulnerabilidad.severity"
          :pt="{ root: 'whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-semibold' }"
        />
      </div>

      <div class="mt-6 grid grid-cols-1 items-center gap-6 sm:grid-cols-2">
        <div class="relative mx-auto h-44 w-44">
          <Chart
            type="doughnut"
            :data="datosGrafico"
            :options="opcionesGrafico"
            class="h-full w-full"
          />
          <div
            class="pointer-events-none absolute inset-0 flex flex-col items-center justify-center"
          >
            <span class="text-4xl font-extrabold tracking-tight text-slate-900 dark:text-slate-50">{{ indice }}</span>
            <span
              class="mt-0.5 text-center text-[10px] font-semibold uppercase leading-tight tracking-wider text-slate-400 dark:text-slate-500"
            >
              Índice total<br />(de 100)
            </span>
          </div>
        </div>

        <ul class="space-y-2">
          <li
            v-for="socio in socios"
            :key="socio.clave"
            class="flex items-center justify-between rounded-lg p-2 transition-colors hover:bg-slate-50 dark:hover:bg-slate-800/60"
          >
            <span class="flex items-center gap-2.5">
              <span
                class="h-3 w-3 shrink-0 rounded-full"
                :style="{ backgroundColor: socio.color }"
              />
              <span class="text-xs font-semibold text-slate-700 dark:text-slate-300">{{ socio.nombre }}</span>
            </span>
            <span class="text-xs font-bold text-slate-900 dark:text-slate-50">{{ socio.porcentaje }}%</span>
          </li>
        </ul>
      </div>

      <div
        class="mt-6 flex items-center gap-2 border-t border-slate-100 pt-4 text-xs text-slate-500 dark:border-slate-800 dark:text-slate-400"
      >
        <i
          class="pi pi-info-circle shrink-0 cursor-help text-sky-600"
          v-tooltip.bottom="{
            value:
              'Mide la concentración del comercio con estas 3 potencias (EE. UU., China, UE). Un índice bajo no significa que el país esté \'a salvo\' — puede deberse a que su comercio es mayormente con otros países, no a su situación política o económica general.',
            class: 'tooltip-ancho',
          }"
        />
        <p>
          Este índice combina dependencia económica ({{ pesos.economico }}%) y estratégica
          ({{ pesos.estrategico }}%)
        </p>
      </div>
    </template>
  </Card>
</template>
