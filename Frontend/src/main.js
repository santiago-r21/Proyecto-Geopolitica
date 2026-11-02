import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Aura from '@primeuix/themes/aura'
import ToastService from 'primevue/toastservice'
import Tooltip from 'primevue/tooltip'

import 'primeicons/primeicons.css'
import 'flag-icons/css/flag-icons.min.css'
import 'leaflet/dist/leaflet.css'
import './style.css'

import App from './App.vue'
import { cargarTema } from './services/tema.js'

// Se aplica antes de montar (no dentro del store) para que la página nunca
// arranque en claro y salte a oscuro un instante después.
document.documentElement.classList.toggle('app-dark', cargarTema())

const app = createApp(App)

app.use(createPinia())
app.use(PrimeVue, {
  theme: {
    preset: Aura,
    options: {
      darkModeSelector: '.app-dark',
      cssLayer: {
        name: 'primevue',
        order: 'theme, base, primevue, components, utilities',
      },
    },
  },
})
app.use(ToastService)
app.directive('tooltip', Tooltip)

app.mount('#app')
