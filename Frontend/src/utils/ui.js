/**
 * Constantes de presentación compartidas entre componentes.
 * Las clases se escriben completas (no concatenadas) para que Tailwind las detecte.
 */

/** Color del chip de icono por "acento" de categoría. */
export const clasesAcento = {
  sky: 'bg-sky-50 text-sky-600',
  emerald: 'bg-emerald-50 text-emerald-600',
  indigo: 'bg-indigo-50 text-indigo-600',
  amber: 'bg-amber-50 text-amber-600',
  purple: 'bg-purple-50 text-purple-600',
  teal: 'bg-teal-50 text-teal-600',
  rose: 'bg-rose-50 text-rose-600',
}

/** Passthrough compartido para las tarjetas grandes (PrimeVue Card). */
export const ptTarjeta = {
  root: 'h-full rounded-2xl border border-slate-200/80 bg-white shadow-xs',
  body: 'flex h-full flex-col p-6',
  caption: 'gap-1',
  content: 'flex-1',
}

/** Texto de ayuda para métricas estimadas (no provienen directamente de la OEC). */
export const AYUDA_ESTIMADO =
  'Estimado a partir de la concentración comercial (sin fuente directa en la OEC).'
