<script setup>
import Toast from 'primevue/toast'
import DashboardView from './views/DashboardView.vue'
</script>

<template>
  <div class="min-h-screen w-full bg-slate-50 text-slate-800">
    <DashboardView />
    <Toast position="top-right" />
  </div>
</template>
