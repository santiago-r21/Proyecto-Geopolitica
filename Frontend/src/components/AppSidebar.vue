<script setup>
import { ref } from 'vue'
import Menu from 'primevue/menu'
import Avatar from 'primevue/avatar'

/** Barra lateral fija: marca + navegación + pie. Siempre visible (app de escritorio). */
const seccionActiva = ref('inicio')

const enlaces = [
  { clave: 'inicio', etiqueta: 'Inicio', icono: 'pi pi-home' },
  { clave: 'paises', etiqueta: 'Países', icono: 'pi pi-globe' },
  { clave: 'comparar', etiqueta: 'Comparar', icono: 'pi pi-chart-bar' },
  { clave: 'sectores', etiqueta: 'Sectores', icono: 'pi pi-th-large' },
  { clave: 'riesgos', etiqueta: 'Riesgos', icono: 'pi pi-exclamation-triangle' },
]

const menu = enlaces.map((enlace) => ({
  ...enlace,
  command: () => {
    seccionActiva.value = enlace.clave
  },
}))
</script>

<template>
  <aside class="flex w-64 shrink-0 flex-col justify-between border-r border-slate-200 bg-white shadow-sm">
    <div>
      <div class="flex items-center gap-3 border-b border-slate-100 p-6">
        <span
          class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-sky-100 bg-sky-50 text-sky-600 shadow-sm"
        >
          <i class="pi pi-globe text-lg" />
        </span>
        <div class="min-w-0">
          <h1 class="text-sm font-bold leading-tight text-slate-900">
            ¿De quién depende este país?
          </h1>
          <p class="mt-0.5 text-[11px] font-medium text-slate-400">Inteligencia Geopolítica</p>
        </div>
      </div>

      <Menu
        :model="menu"
        class="w-full border-0 bg-transparent p-4"
        :pt="{ list: 'flex flex-col gap-1.5', separator: 'hidden', itemContent: 'rounded-xl' }"
      >
        <template #item="{ item, props }">
          <a
            v-bind="props.action"
            class="group flex items-center gap-3 rounded-xl px-3.5 py-3 text-sm transition-colors"
            :class="
              seccionActiva === item.clave
                ? 'border border-sky-200/70 bg-sky-50 font-semibold text-sky-700 shadow-sm'
                : 'border border-transparent font-medium text-slate-600 hover:bg-slate-50 hover:text-slate-900'
            "
          >
            <span
              class="rounded-lg p-1.5 transition-colors"
              :class="
                seccionActiva === item.clave
                  ? 'bg-sky-100 text-sky-700'
                  : 'text-slate-400 group-hover:bg-slate-100 group-hover:text-slate-600'
              "
            >
              <i :class="item.icono" class="block text-base" />
            </span>
            <span>{{ item.etiqueta }}</span>
          </a>
        </template>
      </Menu>
    </div>

    <div class="border-t border-slate-100 p-4">
      <div class="flex items-center gap-3 rounded-xl border border-slate-100 bg-slate-50 p-2">
        <Avatar
          label="OE"
          shape="circle"
          :pt="{ root: 'bg-slate-200 text-slate-700 text-xs font-bold' }"
        />
        <div class="min-w-0 flex-1">
          <p class="truncate text-xs font-semibold text-slate-800">Observatorio Estratégico</p>
          <p class="text-[10px] text-slate-400">Fuente: OEC · BACI</p>
        </div>
      </div>
    </div>
  </aside>
</template>
