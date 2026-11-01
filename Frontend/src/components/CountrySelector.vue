<script setup>
import Select from 'primevue/select'
import CountryFlag from './CountryFlag.vue'
import { paisPorCodigo, PAISES } from '../services/oec/reference.js'

/** Selector de país (PrimeVue Select). El v-model es el código ('CO', 'PE'...). */
const codigo = defineModel({ type: String, required: true })

const isoDe = (c) => paisPorCodigo[c]?.iso ?? null
const nombreDe = (c) => paisPorCodigo[c]?.nombre ?? c
</script>

<template>
  <Select
    v-model="codigo"
    :options="PAISES"
    option-label="nombre"
    option-value="codigo"
    aria-label="Selecciona un país"
    class="w-full sm:w-56"
    :pt="{ label: 'flex items-center gap-2 text-sm font-semibold text-slate-800' }"
  >
    <template #value="{ value }">
      <span class="flex items-center gap-2">
        <CountryFlag :iso="isoDe(value)" size="w-5 h-[0.9375rem]" />
        <span>{{ nombreDe(value) }}</span>
      </span>
    </template>
    <template #option="{ option }">
      <span class="flex items-center gap-2">
        <CountryFlag :iso="option.iso" size="w-5 h-[0.9375rem]" />
        <span>{{ option.nombre }}</span>
      </span>
    </template>
  </Select>
</template>
