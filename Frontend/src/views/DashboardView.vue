<script setup>
import { watch } from 'vue'
import { storeToRefs } from 'pinia'
import Message from 'primevue/message'
import { useToast } from 'primevue/usetoast'

import CountryTabs from '../components/CountryTabs.vue'
import CountryHeader from '../components/CountryHeader.vue'
import CountrySelector from '../components/CountrySelector.vue'
import DependencyIndexCard from '../components/DependencyIndexCard.vue'
import PartnersCard from '../components/PartnersCard.vue'
import DependencyCategories from '../components/DependencyCategories.vue'
import PartnerDetail from '../components/PartnerDetail.vue'
import CardSkeleton from '../components/states/CardSkeleton.vue'
import ErrorState from '../components/states/ErrorState.vue'
import MapaDependencia from '../components/MapaDependencia.vue'
import ComparacionDependencia from '../components/ComparacionDependencia.vue'
import { useDashboardStore } from '../stores/dashboard.js'

const store = useDashboardStore()
const {
  codigoPais,
  pestanaActiva,
  datosActuales: datos,
  cargandoActual: cargando,
  errorActual: error,
  sociosActuales,
  cargandoSociosActual,
  errorSociosActual,
  comparacionActual,
  cargandoComparacionActual,
  errorComparacionActual,
} = storeToRefs(store)
const toast = useToast()

watch(datos, (pais) => {
  if (!pais) return
  toast.add({
    severity: 'success',
    summary: `${pais.nombre} · datos actualizados`,
    detail: `Fuente OEC · ${pais.meta.dataset} · año ${pais.meta.anio}`,
    life: 2800,
  })
})

watch(error, (err) => {
  if (!err) return
  toast.add({
    severity: 'error',
    summary: 'No se pudo consultar el servidor',
    detail: err.message ?? 'Inténtalo de nuevo en unos segundos.',
    life: 4000,
  })
})

const pestanasPendientes = [
  { value: 'economica', titulo: 'Dependencia económica', icono: 'pi pi-dollar' },
  { value: 'estrategica', titulo: 'Dependencia estratégica', icono: 'pi pi-shield' },
  { value: 'sectores', titulo: 'Análisis por sectores', icono: 'pi pi-th-large' },
]

const nombrePais = () => datos.value?.nombre ?? store.catalogoPorCodigo[codigoPais.value]?.nombre ?? ''
const isoPais = () => datos.value?.iso ?? store.catalogoPorCodigo[codigoPais.value]?.iso2 ?? null
</script>

<template>
  <div class="h-screen overflow-y-auto overflow-x-hidden bg-slate-50/60">
    <CountryTabs v-model="pestanaActiva">
      <!-- Cabecera sticky: país + selector -->
      <template #header>
        <CountryHeader :nombre="nombrePais()" :iso="isoPais()" :subtitulo="datos?.subtitulo ?? ''">
          <CountrySelector v-model="codigoPais" />
        </CountryHeader>
      </template>

      <!-- RESUMEN -->
      <template #resumen>
        <div class="mx-auto max-w-7xl space-y-7 p-4 sm:p-6 lg:p-8">
          <ErrorState v-if="error && !datos" :mensaje="error.message" @reintentar="store.recargar" />

          <template v-else-if="!datos">
            <section class="grid grid-cols-1 gap-6 lg:grid-cols-12">
              <CardSkeleton variante="indice" />
              <CardSkeleton variante="lista" />
            </section>
            <CardSkeleton variante="rejilla" />
            <CardSkeleton variante="detalle" />
          </template>

          <div
            v-else
            class="space-y-7 transition-opacity"
            :class="{ 'pointer-events-none opacity-60': cargando }"
          >
            <Message v-if="error" severity="warn" :closable="false">
              Mostrando los últimos datos disponibles. La actualización falló: {{ error.message }}
            </Message>

            <section class="grid grid-cols-1 gap-6 lg:grid-cols-12">
              <div class="lg:col-span-7">
                <DependencyIndexCard
                  :indice="datos.indiceDependencia"
                  :vulnerabilidad="datos.vulnerabilidad"
                  :socios="datos.socios"
                  :pesos="datos.pesos"
                />
              </div>
              <div class="lg:col-span-5">
                <PartnersCard :socios="datos.socios" />
              </div>
            </section>

            <DependencyCategories :categorias="datos.categorias" />

            <PartnerDetail :socios="datos.detalleSocios" />

            <p class="pt-1 text-center text-[11px] text-slate-400">
              Fuente: OEC · {{ datos.meta.dataset }} · año {{ datos.meta.anio }}. El índice de
              dependencia, la vulnerabilidad y las métricas marcadas «est.» son estimaciones
              derivadas de la concentración comercial.
            </p>
          </div>
        </div>
      </template>

      <!-- MAPA -->
      <template #mapa>
        <div class="mx-auto max-w-7xl p-4 sm:p-6 lg:p-8">
          <MapaDependencia
            :socios="sociosActuales"
            :cargando="cargandoSociosActual"
            :error="errorSociosActual"
          />
        </div>
      </template>

      <!-- COMPARAR -->
      <template #comparar>
        <div class="mx-auto max-w-7xl p-4 sm:p-6 lg:p-8">
          <ComparacionDependencia
            :comparacion="comparacionActual"
            :cargando="cargandoComparacionActual"
            :error="errorComparacionActual"
          />
        </div>
      </template>

      <!-- Pestañas preparadas para contenido futuro -->
      <template v-for="p in pestanasPendientes" #[p.value] :key="p.value">
        <div class="mx-auto max-w-7xl p-4 sm:p-6 lg:p-8">
          <div
            class="flex flex-col items-center justify-center gap-3 rounded-2xl border border-dashed border-slate-300 bg-white p-16 text-center"
          >
            <span
              class="flex h-12 w-12 items-center justify-center rounded-xl bg-sky-50 text-sky-600"
            >
              <i :class="p.icono" class="text-xl" />
            </span>
            <h3 class="text-base font-bold text-slate-900">{{ p.titulo }}</h3>
            <p class="max-w-sm text-sm text-slate-500">
              Esta sección mostrará el desglose de {{ nombrePais() }} próximamente. La estructura ya
              está preparada para recibir más datos de la OEC.
            </p>
          </div>
        </div>
      </template>
    </CountryTabs>
  </div>
</template>
