<script setup>
import Select from 'primevue/select'
import Button from 'primevue/button'
import CountryFlag from './CountryFlag.vue'
import { useDashboardStore } from '../stores/dashboard.js'
import { ACCESOS_RAPIDOS } from '../services/oec/reference.js'

/** Selector de país: accesos rápidos + buscador sobre el catálogo completo. */
const codigo = defineModel({ type: String, required: true })

const store = useDashboardStore()
</script>

<template>
  <div class="flex flex-col gap-1.5 sm:flex-row sm:items-center sm:gap-2">
    <div class="flex items-center gap-1">
      <button
        v-for="id in ACCESOS_RAPIDOS"
        :key="id"
        type="button"
        :title="store.catalogoPorCodigo[id]?.nombre ?? id"
        class="flex h-8 w-8 items-center justify-center rounded-lg border transition-colors"
        :class="
          codigo === id
            ? 'border-sky-300 bg-sky-50'
            : 'border-transparent hover:border-slate-200 hover:bg-slate-50'
        "
        @click="codigo = id"
      >
        <CountryFlag :iso="store.catalogoPorCodigo[id]?.iso2" size="w-5 h-[0.9375rem]" />
      </button>
    </div>

    <div v-if="store.catalogoError" class="flex items-center gap-2 text-xs text-rose-600">
      <span>No se pudo cargar el catálogo de países.</span>
      <Button
        label="Reintentar"
        size="small"
        text
        severity="danger"
        class="p-0 text-xs underline"
        @click="store.cargarCatalogo()"
      />
    </div>

    <Select
      v-else
      v-model="codigo"
      :options="store.catalogo"
      option-label="nombre"
      option-value="codigo"
      filter
      :filter-fields="['nombre', 'iso2']"
      filter-placeholder="Buscar país..."
      empty-filter-message="Ningún país coincide."
      :loading="store.cargandoCatalogo"
      aria-label="Busca un país"
      class="w-full sm:w-64"
      :pt="{ label: 'flex items-center gap-2 text-sm font-semibold text-slate-800' }"
    >
      <template #value="{ value }">
        <span class="flex items-center gap-2">
          <CountryFlag :iso="store.catalogoPorCodigo[value]?.iso2" size="w-5 h-[0.9375rem]" />
          <span>{{ store.catalogoPorCodigo[value]?.nombre ?? value }}</span>
        </span>
      </template>
      <template #option="{ option }">
        <span class="flex items-center gap-2">
          <CountryFlag :iso="option.iso2" size="w-5 h-[0.9375rem]" />
          <span>{{ option.nombre }}</span>
        </span>
      </template>
    </Select>
  </div>
</template>
