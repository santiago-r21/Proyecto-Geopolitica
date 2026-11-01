<script setup>
import { computed } from 'vue'
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import { clasesAcento, AYUDA_ESTIMADO } from '../utils/ui.js'

const props = defineProps({
  categoria: { type: Object, required: true },
})

const claseChip = computed(() => clasesAcento[props.categoria.acento] ?? clasesAcento.sky)

const ayuda = computed(() =>
  props.categoria.estimado
    ? AYUDA_ESTIMADO
    : `${props.categoria.nombre}: concentración ${props.categoria.valor}% · nivel ${props.categoria.etiqueta}`,
)
</script>

<template>
  <Card
    v-tooltip.top="ayuda"
    :pt="{
      root: 'h-full rounded-xl border border-slate-200/80 bg-white shadow-xs transition-colors hover:border-slate-300',
      body: 'flex h-full flex-col justify-between gap-3 p-4',
    }"
  >
    <template #content>
      <div>
        <span
          class="mb-3 flex h-9 w-9 items-center justify-center rounded-lg"
          :class="claseChip"
        >
          <i :class="categoria.icono" class="text-base" />
        </span>
        <p class="flex items-center gap-1 text-xs font-medium text-slate-500">
          {{ categoria.nombre }}
          <span
            v-if="categoria.estimado"
            class="rounded bg-slate-100 px-1 text-[9px] font-bold uppercase text-slate-400"
          >
            est.
          </span>
        </p>
        <p class="mt-0.5 text-xl font-bold text-slate-900">{{ categoria.valor }}%</p>
      </div>
      <Tag
        :value="categoria.etiqueta"
        :severity="categoria.severity"
        :pt="{ root: 'w-fit rounded px-2 py-0.5 text-[11px] font-bold' }"
      />
    </template>
  </Card>
</template>
