<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import AutoComplete from 'primevue/autocomplete'
import Select from 'primevue/select'
import Button from 'primevue/button'
import CountryFlag from './CountryFlag.vue'
import { useDashboardStore } from '../stores/dashboard.js'

/**
 * Selector de país: buscador sobre el catálogo completo + favoritos
 * (localStorage). El botón de guardar/quitar favorito vive en CountryHeader.vue
 * (junto a la bandera grande), no acá.
 */
const codigo = defineModel({ type: String, required: true })

const store = useDashboardStore()

const paisesFavoritos = computed(() =>
  store.favoritos.map((id) => store.catalogoPorCodigo[id]).filter(Boolean),
)

// El buscador es solo para encontrar un país nuevo, no para mostrar el actual
// (eso ya lo muestra la cabecera y el select de favoritos) — por eso siempre
// vuelve a quedar vacío (placeholder) después de elegir. Se resetea con un
// watch sobre `codigo` (no dentro de `seleccionar`) para que también se
// limpie cuando el país cambia por otro camino, como el select de favoritos.
// El AutoComplete de PrimeVue no siempre resincroniza su texto visible solo
// con el v-model puesto en null desde afuera, así que además se limpia el
// <input> nativo por DOM directamente (sin remontar el componente — eso
// rompía la selección con Enter, que hace varios pasos internos después de
// elegir una opción).
const buscadorRef = ref(null)
const paisBuscado = ref(null)
watch(codigo, async () => {
  paisBuscado.value = null
  await nextTick()
  const input = buscadorRef.value?.$el?.querySelector('input')
  if (input) input.value = ''
})

const sugerencias = ref([])
function buscar({ query }) {
  const q = query.trim().toLowerCase()
  sugerencias.value = !q
    ? store.catalogo
    : store.catalogo.filter((p) => p.nombre.toLowerCase().includes(q) || p.iso2.includes(q))
}

function seleccionar({ value }) {
  codigo.value = value.codigo
}
</script>

<template>
  <div class="flex w-full flex-col gap-3 sm:flex-row sm:items-end">
    <!-- Buscador: catálogo completo -->
    <div class="flex min-w-0 flex-1 flex-col gap-1">
      <span class="text-[10px] font-semibold uppercase tracking-wide text-slate-400">Buscar</span>

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
        ref="buscadorRef"
        v-model="paisBuscado"
        :suggestions="sugerencias"
        option-label="nombre"
        placeholder="Buscar país..."
        :loading="store.cargandoCatalogo"
        :delay="150"
        complete-on-focus
        force-selection
        fluid
        scroll-height="18rem"
        empty-search-message="Ningún país coincide."
        aria-label="Busca un país"
        input-class="text-sm font-semibold text-slate-800 dark:text-slate-100"
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
    </div>

    <!-- Favoritos -->
    <div class="flex flex-col gap-1">
      <span class="text-[10px] font-semibold uppercase tracking-wide text-slate-400">Favoritos</span>

      <Select
        v-model="codigo"
        :options="paisesFavoritos"
        option-label="nombre"
        option-value="codigo"
        placeholder="Sin favoritos"
        empty-message="Guarda un país para verlo aquí."
        aria-label="Tus países favoritos"
        class="w-full sm:w-44"
        :pt="{ label: 'flex items-center gap-2 text-sm font-semibold text-slate-800 dark:text-slate-100' }"
      >
        <template #value="{ value }">
          <span v-if="store.esFavorito(value)" class="flex items-center gap-2">
            <CountryFlag :iso="store.catalogoPorCodigo[value]?.iso2" size="w-5 h-[0.9375rem]" />
            <span>{{ store.catalogoPorCodigo[value]?.nombre ?? value }}</span>
          </span>
          <span v-else class="text-slate-400 dark:text-slate-500">Elige un favorito</span>
        </template>
        <template #option="{ option }">
          <span class="flex items-center gap-2">
            <CountryFlag :iso="option.iso2" size="w-5 h-[0.9375rem]" />
            <span>{{ option.nombre }}</span>
          </span>
        </template>
      </Select>
    </div>
  </div>
</template>
