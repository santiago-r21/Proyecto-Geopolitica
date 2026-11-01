import { OEC_CUBE, OEC_YEAR } from './client.js'

/**
 * Constructores de las URLs relativas de las 4 consultas que alimentan el dashboard.
 * Todas filtran por un único año (`OEC_YEAR`) y devuelven `Trade Value` en USD.
 */

function construirUrl(params) {
  // Formato verificado contra la OEC: espacios como `+`, y `,` `:` `.` literales
  // dentro de los valores (drilldowns / include / sort).
  const qs = Object.entries({ cube: OEC_CUBE, measures: 'Trade Value', ...params })
    .map(([clave, valor]) => `${clave}=${String(valor).replace(/ /g, '+')}`)
    .join('&')
  return `data.jsonrecords?${qs}`
}

/** 1. Exportaciones del país, desglosadas por país socio (importador). */
export const exportacionesPorSocio = (oecId) =>
  construirUrl({
    drilldowns: 'Importer Country',
    include: `Exporter Country:${oecId};Year:${OEC_YEAR}`,
    sort: 'Trade Value.desc',
  })

/** 2. Importaciones del país, desglosadas por país socio (exportador). */
export const importacionesPorSocio = (oecId) =>
  construirUrl({
    drilldowns: 'Exporter Country',
    include: `Importer Country:${oecId};Year:${OEC_YEAR}`,
    sort: 'Trade Value.desc',
  })

/** 3. Exportaciones del país por Sección HS × país socio. */
export const exportacionesPorSeccion = (oecId) =>
  construirUrl({
    drilldowns: 'Section,Importer Country',
    include: `Exporter Country:${oecId};Year:${OEC_YEAR}`,
  })

/** 4. Importaciones del país por Sección HS × país socio. */
export const importacionesPorSeccion = (oecId) =>
  construirUrl({
    drilldowns: 'Section,Exporter Country',
    include: `Importer Country:${oecId};Year:${OEC_YEAR}`,
  })
