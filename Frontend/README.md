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

No hace falta backend ni proxy: la OEC API responde con `Access-Control-Allow-Origin: *`.

> En PowerShell con la _execution policy_ en `Restricted`, usa `npm.cmd run dev` o ejecuta una vez
> `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.

## Configuración (`.env`)

| Variable        | Por defecto                          | Descripción                            |
| --------------- | ------------------------------------ | ------------------------------------- |
| `VITE_OEC_BASE` | `https://api-v2.oec.world/tesseract` | Base de la OEC API (motor Tesseract)  |
| `VITE_OEC_CUBE` | `trade_i_baci_a_22`                  | Cubo BACI HS-2022 (abierto, sin token) |
| `VITE_OEC_YEAR` | `2023`                               | Año consultado (2022–2024 disponibles) |

## Estructura

```
src/
├─ App.vue                     Layout: sidebar + dashboard + <Toast>
├─ views/DashboardView.vue     Orquesta datos → estados carga/error → componentes
├─ composables/
│  └─ usarDatosPais.js         watch(código) → consulta OEC → transformar → { datos, cargando, error, recargar }
├─ services/oec/
│  ├─ client.js                fetch + AbortController + errores legibles (OecError)
│  ├─ queries.js               las 4 consultas (export/import por socio y por sección×socio)
│  ├─ transform.js             filas OEC → modelo del dashboard (TODAS las fórmulas aquí)
│  └─ reference.js             PAISES, ids OEC, CLAVES_UE27, SOCIOS, NIVELES, CATEGORIAS, AREAS_SOCIO
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

Modelo que entrega `transformar()` / consume la vista:
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

Todos los umbrales y pesos están centralizados y comentados en
[`src/services/oec/transform.js`](src/services/oec/transform.js).

## Notas

- App de **escritorio**: la barra lateral está siempre visible (no hay Drawer ni menú móvil). El
  layout se adapta al redimensionar la ventana (2 columnas → 1, categorías 6→3→2).
- Sin persistencia: cada cambio de país dispara consultas nuevas. El último resultado se mantiene en
  memoria sólo para no parpadear durante la recarga.
- Peticiones en vuelo se cancelan (`AbortController`) al cambiar de país.
