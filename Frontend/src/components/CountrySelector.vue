<script setup>
import { computed, ref } from 'vue'
import AutoComplete from 'primevue/autocomplete'
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

// El buscador es solo para encontrar un país nuevo, no para mostrar el actual
// (eso ya lo muestra la cabecera y el select de favoritos) — por eso siempre
// vuelve a quedar vacío (placeholder) después de elegir.
const paisBuscado = ref(null)

const sugerencias = ref([])
function buscar({ query }) {
  const q = query.trim().toLowerCase()
  sugerencias.value = !q
    ? store.catalogo
    : store.catalogo.filter((p) => p.nombre.toLowerCase().includes(q) || p.iso2.includes(q))
}

function seleccionar({ value }) {
  codigo.value = value.codigo
  paisBuscado.value = null
}
</script>

<template>
  <div class="flex flex-col gap-1.5 sm:flex-row sm:items-center sm:gap-2">
    <!-- Guardar/quitar de favoritos (toggle) -->
    <Button
      :icon="store.esFavorito(codigo) ? 'pi pi-star-fill' : 'pi pi-star'"
      :severity="store.esFavorito(codigo) ? 'warn' : 'secondary'"
      text
      rounded
      class="shrink-0"
      :aria-label="store.esFavorito(codigo) ? 'Quitar de favoritos' : 'Guardar en favoritos'"
      v-tooltip.top="store.esFavorito(codigo) ? 'Quitar de favoritos' : 'Guardar en favoritos'"
      @click="store.alternarFavorito(codigo)"
    />

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

    <AutoComplete
      v-else
      v-model="paisBuscado"
      :suggestions="sugerencias"
      option-label="nombre"
      placeholder="Buscar país..."
      :loading="store.cargandoCatalogo"
      :delay="150"
      complete-on-focus
      force-selection
      empty-search-message="Ningún país coincide."
      aria-label="Busca un país"
      class="w-full sm:w-48"
      input-class="w-full text-sm font-semibold text-slate-800"
      @complete="buscar"
      @item-select="seleccionar"
    >
      <template #option="{ option }">
        <span class="flex items-center gap-2">
          <CountryFlag :iso="option.iso2" size="w-5 h-[0.9375rem]" />
          <span>{{ option.nombre }}</span>
        </span>
      </template>
    </AutoComplete>

    <!-- Favoritos -->
    <Select
      v-model="codigo"
      :options="paisesFavoritos"
      option-label="nombre"
      option-value="codigo"
      placeholder="Sin favoritos"
      empty-message="Guarda un país para verlo aquí."
      aria-label="Tus países favoritos"
      class="w-full sm:w-40"
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
