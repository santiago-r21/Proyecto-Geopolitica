<script setup>
import Toast from 'primevue/toast'
import AppSidebar from './components/AppSidebar.vue'
import DashboardView from './views/DashboardView.vue'
</script>

<template>
  <div class="flex min-h-screen w-full bg-slate-50 text-slate-800">
    <AppSidebar />

    <div class="flex min-w-0 flex-1 flex-col">
      <DashboardView />
    </div>

    <Toast position="top-right" />
  </div>
</template>
