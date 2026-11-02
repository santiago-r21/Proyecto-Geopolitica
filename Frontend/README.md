# ¿De quién depende este país? · Inteligencia Geopolítica

Dashboard **web de escritorio** que analiza la **dependencia económica y estratégica** de un país
respecto a sus principales socios internacionales (Estados Unidos, China, Unión Europea y resto del
mundo), con datos **en vivo** de la [OEC – Observatory of Economic Complexity](https://oec.world).

## Stack

- **Vue 3** + `<script setup>` + Composition API
- **PrimeVue 4** (preset Aura) — Select, Tabs, Card, Tag, ProgressBar, Chart, Toast, Menu, Skeleton, Message
- **Tailwind CSS v4** (`@tailwindcss/vite`) para todo el layout y los estilos
- **Chart.js** (vía el componente `Chart` de PrimeVue) para el donut
- **flag-icons** para las banderas
- **Vite 6**

## Requisitos

- **Node.js ≥ 18** y npm (instalado: Node 24 LTS en `C:\Program Files\nodejs`).

## Puesta en marcha

```bash
npm install
npm run dev       # http://localhost:5173
npm run build     # build de producción en dist/
npm run preview   # sirve dist/
```

**Necesita el backend corriendo** (`Backend/Proyecto/Proyecto`, Spring Boot + Postgres) en
`http://localhost:8080` — es quien habla con la OEC, calcula el modelo del dashboard y lo cachea.
El frontend solo le pega a nuestro propio backend, nunca a la OEC directamente.

> En PowerShell con la _execution policy_ en `Restricted`, usa `npm.cmd run dev` o ejecuta una vez
> `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.

## Configuración (`.env`)

| Variable        | Por defecto             | Descripción                                   |
| --------------- | ------------------------ | --------------------------------------------- |
| `VITE_API_BASE` | `http://localhost:8080`  | Base de nuestro backend                       |
| `VITE_OEC_YEAR` | `2023`                   | Año consultado (2022–2024 disponibles)        |

## Estructura

```
src/
├─ App.vue                     Layout: sidebar + dashboard + <Toast>
├─ views/DashboardView.vue     Orquesta datos → estados carga/error → componentes
├─ stores/dashboard.js         Pinia: país/pestaña seleccionados + caché de sesión por país,
│                              watch(código) → consulta el backend si no está cacheado
├─ services/
│  ├─ api/client.js            fetch a nuestro backend + errores legibles (ApiError)
│  └─ oec/reference.js         PAISES (accesos rápidos) e ids OEC
├─ components/
│  ├─ AppSidebar              barra lateral fija (marca + Menu + pie)
│  ├─ CountryHeader / CountrySelector
│  ├─ CountryTabs             Tabs/TabList/Tab/TabPanels/TabPanel + encabezado sticky
│  ├─ DependencyIndexCard     donut (Chart) + leyenda + índice
│  ├─ PartnersCard            socios + ProgressBar
│  ├─ DependencyCategories / CategoryCard
│  ├─ PartnerDetail / PartnerDetailCard
│  ├─ CountryFlag             bandera (flag-icons) con fallback a globo
│  └─ states/ CardSkeleton, ErrorState
└─ utils/ui.js                clases de presentación compartidas
```

### Convención de nombres

La **lógica de dominio** (socios, categorías, dependencia, vulnerabilidad, países, áreas, niveles)
usa identificadores y propiedades en **español**. Lo técnico/genérico y las APIs de Vue/PrimeVue
(`fetch`, `watch`, `ref`, `AbortController`, `severity`, `value`…) se mantienen en inglés. Los
nombres de archivo de componente siguen la convención PascalCase de Vue.

Modelo que entrega `GET /api/paises/{codigo}/dependencia` / consume la vista:
`{ codigo, nombre, iso, subtitulo, indiceDependencia, vulnerabilidad{etiqueta,severity},
pesos{economico,estrategico}, socios[], categorias[], detalleSocios[], meta{fuente,dataset,anio,…} }`.

## Modelo de datos: qué es real y qué es derivado

La OEC sólo publica **comercio de bienes** (exportaciones/importaciones por país socio y por
producto). A partir de eso:

| Bloque del dashboard                              | Origen                                                                               |
| ------------------------------------------------- | ----------------------------------------------------------------------------------- |
| Principales Socios · donut · %                    | **Real**: cuota de cada bloque sobre el comercio total (export + import) del año.    |
| Detalle por Socio → Exportaciones / Importaciones | **Real**: cuota del socio en ese flujo.                                             |
| Detalle por Socio → Tecnología / Energía / …      | **Real**: cuota del socio en esas secciones HS.                                     |
| Dependencia por categoría (Comercio, Tecnología, Energía, Recursos) | **Real**: parte de ese dominio que depende de EE.UU. + China + UE. |
| Índice de dependencia (0–100) y Vulnerabilidad    | **Derivado**: índice compuesto de concentración (`0.55·potencia_líder + 0.30·suma_potencias + 0.15·HHI`, con curva `^0.8`). No es un indicador oficial. |
| Categorías «Inversión» e «Infraestructura», y áreas marcadas `est.` | **Estimado**: sin fuente directa en la OEC; se derivan del índice / de la cuota global del socio. |

Todos los umbrales y pesos están centralizados y comentados en el backend
(`Backend/Proyecto/Proyecto/.../service/DependenciaTransformer.java` y `Referencia.java`).

## Notas

- App de **escritorio**: la barra lateral está siempre visible (no hay Drawer ni menú móvil). El
  layout se adapta al redimensionar la ventana (2 columnas → 1, categorías 6→3→2).
- Caché de sesión (Pinia, `stores/dashboard.js`): volver a un país ya consultado en esta pestaña no
  dispara una nueva petición; se pierde al recargar la página (F5), es intencional.
- Peticiones en vuelo se cancelan (`AbortController`) al forzar una recarga.
