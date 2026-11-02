<script setup>
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'

/**
 * Envoltura de pestañas del dashboard. El encabezado (país + selector) va en el slot
 * `#header` y queda dentro de `<Tabs>` para que el bloque sticky incluya las pestañas.
 * Cada panel se expone como un slot con nombre = value de la pestaña.
 */
const activa = defineModel({ type: String, default: 'resumen' })

const pestanas = [
  { value: 'resumen', etiqueta: 'Resumen' },
  { value: 'mapa', etiqueta: 'Mapa' },
  { value: 'comparar', etiqueta: 'Comparar' },
  { value: 'economica', etiqueta: 'Económica' },
  { value: 'estrategica', etiqueta: 'Estratégica' },
  { value: 'sectores', etiqueta: 'Sectores' },
]

const CLASE_BASE =
  'relative -mb-px cursor-pointer border-0 border-b-2 bg-transparent px-1 py-3.5 text-sm transition-colors'
const CLASE_ACTIVA = 'border-sky-600 font-bold text-sky-600'
const CLASE_INACTIVA = 'border-transparent font-medium text-slate-500 hover:text-slate-700'

// pt como función: PrimeVue expone `context.active` para cada Tab.
const ptPestana = {
  root: ({ context }) => ({
    class: [CLASE_BASE, context?.active ? CLASE_ACTIVA : CLASE_INACTIVA],
  }),
}
</script>

<template>
  <Tabs v-model:value="activa" :pt="{ root: 'flex min-h-full flex-col' }">
    <div
      class="sticky top-0 z-20 border-b border-slate-200/80 bg-white/95 px-4 backdrop-blur sm:px-6 lg:px-8"
    >
      <slot name="header" />

      <TabList
        :pt="{
          root: 'overflow-x-auto border-0 bg-transparent',
          content: 'border-0',
          tabList: 'flex gap-6 border-0 bg-transparent',
          activeBar: 'hidden',
          prevButton: 'hidden',
          nextButton: 'hidden',
        }"
      >
        <Tab v-for="p in pestanas" :key="p.value" :value="p.value" :pt="ptPestana">
          {{ p.etiqueta }}
        </Tab>
      </TabList>
    </div>

    <TabPanels :pt="{ root: 'bg-transparent p-0' }">
      <TabPanel v-for="p in pestanas" :key="p.value" :value="p.value" :pt="{ root: 'outline-none' }">
        <slot :name="p.value" />
      </TabPanel>
    </TabPanels>
  </Tabs>
</template>
