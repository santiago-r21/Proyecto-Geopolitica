<script setup>
import { computed } from 'vue'
import Select from 'primevue/select'
import Button from 'primevue/button'
import CountryFlag from './CountryFlag.vue'
import { useDashboardStore } from '../stores/dashboard.js'

/** Selector de país: buscador sobre el catálogo completo + favoritos (localStorage). */
const codigo = defineModel({ type: String, required: true })

const store = useDashboardStore()

const paisesFavoritos = computed(() =>
  store.favoritos.map((id) => store.catalogoPorCodigo[id]).filter(Boolean),
)
</script>

<template>
  <div class="flex flex-col gap-1.5 sm:flex-row sm:items-center sm:gap-2">
    <!-- Buscador: catálogo completo -->
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
      class="w-full sm:w-56"
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

    <!-- Guardar/quitar de favoritos (toggle) -->
    <Button
      :icon="store.esFavorito(codigo) ? 'pi pi-star-fill' : 'pi pi-star'"
      :severity="store.esFavorito(codigo) ? 'warn' : 'secondary'"
      text
      rounded
      :aria-label="store.esFavorito(codigo) ? 'Quitar de favoritos' : 'Guardar en favoritos'"
      v-tooltip.top="store.esFavorito(codigo) ? 'Quitar de favoritos' : 'Guardar en favoritos'"
      @click="store.alternarFavorito(codigo)"
    />

    <!-- Favoritos -->
    <Select
      v-model="codigo"
      :options="paisesFavoritos"
      option-label="nombre"
      option-value="codigo"
      placeholder="Sin favoritos"
      empty-message="Guarda un país para verlo aquí."
      aria-label="Tus países favoritos"
      class="w-full sm:w-48"
      :pt="{ label: 'flex items-center gap-2 text-sm font-semibold text-slate-800' }"
    >
      <template #value="{ value }">
        <span v-if="store.esFavorito(value)" class="flex items-center gap-2">
          <CountryFlag :iso="store.catalogoPorCodigo[value]?.iso2" size="w-5 h-[0.9375rem]" />
          <span>{{ store.catalogoPorCodigo[value]?.nombre ?? value }}</span>
        </span>
        <span v-else class="text-slate-400">Elige un favorito</span>
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
