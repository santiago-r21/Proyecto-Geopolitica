<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import L from 'leaflet'
import Card from 'primevue/card'
import Message from 'primevue/message'
import { ptTarjeta } from '../utils/ui.js'
import CardSkeleton from './states/CardSkeleton.vue'

const props = defineProps({
  socios: { type: Array, default: null },
  cargando: { type: Boolean, default: false },
  error: { type: Object, default: null },
})

// Buckets de concentración de comercio con un socio real. Pensados para la
// distribución de ~200 países (no las mismas cotas que los 4 bloques del
// resumen, donde un solo bloque puede llegar a ~26-50%).
const NIVELES = [
  { max: 0, color: '#e2e8f0', etiqueta: 'Sin comercio registrado' },
  { max: 5, color: '#a7f3d0', etiqueta: 'Baja (<5%)' },
  { max: 15, color: '#fcd34d', etiqueta: 'Media (5-15%)' },
  { max: Infinity, color: '#f43f5e', etiqueta: 'Alta (>15%)' },
]

function colorPorCuota(cuota) {
  return NIVELES.find((n) => cuota <= n.max)?.color ?? NIVELES[0].color
}

let geojsonMundial = null // se descarga una sola vez y se reutiliza entre países
let mapa = null
let capaPaises = null
let observador = null
const contenedor = ref(null)

const sociosPorIso2 = computed(() => {
  const mapa = {}
  for (const socio of props.socios ?? []) {
    if (socio.iso2) mapa[socio.iso2.toLowerCase()] = socio
  }
  return mapa
})

function estiloFeature(feature) {
  const socio = sociosPorIso2.value[feature.properties.iso2]
  return {
    fillColor: colorPorCuota(socio?.cuotaPorcentaje ?? 0),
    fillOpacity: 0.85,
    color: '#ffffff',
    weight: 0.75,
  }
}

function formatoUsd(valor) {
  return new Intl.NumberFormat('es-CO', { notation: 'compact', maximumFractionDigits: 1 }).format(valor ?? 0)
}

function tooltipFeature(feature, layer) {
  const socio = sociosPorIso2.value[feature.properties.iso2]
  const nombre = socio?.nombre ?? feature.properties.nombre
  const contenido = socio
    ? `<strong>${nombre}</strong><br>${socio.cuotaPorcentaje}% del comercio total<br>` +
      `Exportaciones: US$ ${formatoUsd(socio.exportaciones)} · Importaciones: US$ ${formatoUsd(socio.importaciones)}`
    : `<strong>${nombre}</strong><br>Sin comercio registrado`
  layer.bindTooltip(contenido, { sticky: true })
}

function pintarCapa() {
  if (!mapa || !geojsonMundial) return
  capaPaises?.remove()
  capaPaises = L.geoJSON(geojsonMundial, { style: estiloFeature, onEachFeature: tooltipFeature }).addTo(mapa)
}

onMounted(async () => {
  mapa = L.map(contenedor.value, {
    attributionControl: false,
    zoomControl: true,
    minZoom: 1,
    maxZoom: 6,
  }).setView([15, 0], 2)

  // La pestaña "Mapa" sigue montada (solo oculta) mientras no está activa, así
  // que Leaflet se inicializa con el contenedor en 0×0 y queda en gris hasta
  // que algo fuerce un recálculo. Este observer llama a invalidateSize() cada
  // vez que el contenedor realmente cambia de tamaño (ej. al activar la
  // pestaña), sin que el componente necesite saber nada de pestañas/store.
  observador = new ResizeObserver(() => mapa?.invalidateSize())
  observador.observe(contenedor.value)

  if (!geojsonMundial) {
    const res = await fetch('/data/world-countries.geojson')
    geojsonMundial = await res.json()
  }
  pintarCapa()
})

onUnmounted(() => {
  observador?.disconnect()
  observador = null
  mapa?.remove()
  mapa = null
})

watch(() => props.socios, pintarCapa)
</script>

<template>
  <Card :pt="ptTarjeta">
    <template #content>
      <div class="mb-4 flex flex-wrap items-center justify-between gap-x-3 gap-y-1">
        <h3 class="text-base font-bold text-slate-900">Mapa de socios comerciales</h3>
        <div class="flex items-center gap-3 text-[11px] font-medium text-slate-500">
          <span v-for="n in NIVELES" :key="n.etiqueta" class="flex items-center gap-1.5">
            <span class="h-2.5 w-2.5 rounded-sm" :style="{ backgroundColor: n.color }" />
            {{ n.etiqueta }}
          </span>
        </div>
      </div>

      <div class="relative">
        <div ref="contenedor" class="h-[26rem] w-full overflow-hidden rounded-xl bg-slate-50" />

        <div v-if="cargando && !socios" class="absolute inset-0">
          <CardSkeleton variante="mapa" />
        </div>
        <div v-else-if="error && !socios" class="absolute inset-0 flex items-center bg-white/90 p-4">
          <Message severity="error" :closable="false">
            No se pudieron cargar los socios comerciales: {{ error.message }}
          </Message>
        </div>
      </div>
    </template>
  </Card>
</template>
