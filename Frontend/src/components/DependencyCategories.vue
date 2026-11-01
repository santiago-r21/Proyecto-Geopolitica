<script setup>
import CategoryCard from './CategoryCard.vue'

defineProps({
  categorias: { type: Array, required: true },
})
</script>

<template>
  <section>
    <div class="mb-4 flex items-center justify-between">
      <h3 class="text-base font-bold text-slate-900">Dependencia por categoría</h3>
      <span class="text-xs font-medium text-slate-500">
        {{ categorias.length }} áreas críticas evaluadas
      </span>
    </div>

    <div class="grid grid-cols-2 gap-3.5 md:grid-cols-3 lg:grid-cols-6">
      <CategoryCard v-for="cat in categorias" :key="cat.nombre" :categoria="cat" />
    </div>
  </section>
</template>
