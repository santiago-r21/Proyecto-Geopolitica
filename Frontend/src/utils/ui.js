/**
 * Constantes de presentación compartidas entre componentes.
 * Las clases se escriben completas (no concatenadas) para que Tailwind las detecte.
 */

/** Color del chip de icono por "acento" de categoría. */
export const clasesAcento = {
  sky: 'bg-sky-50 text-sky-600 dark:bg-sky-500/15 dark:text-sky-400',
  emerald: 'bg-emerald-50 text-emerald-600 dark:bg-emerald-500/15 dark:text-emerald-400',
  indigo: 'bg-indigo-50 text-indigo-600 dark:bg-indigo-500/15 dark:text-indigo-400',
  amber: 'bg-amber-50 text-amber-600 dark:bg-amber-500/15 dark:text-amber-400',
  purple: 'bg-purple-50 text-purple-600 dark:bg-purple-500/15 dark:text-purple-400',
  teal: 'bg-teal-50 text-teal-600 dark:bg-teal-500/15 dark:text-teal-400',
  rose: 'bg-rose-50 text-rose-600 dark:bg-rose-500/15 dark:text-rose-400',
}

/** Passthrough compartido para las tarjetas grandes (PrimeVue Card). */
export const ptTarjeta = {
  root: 'h-full rounded-2xl border border-slate-200/80 bg-white shadow-xs dark:border-slate-800 dark:bg-slate-900',
  body: 'flex h-full flex-col p-6',
  caption: 'gap-1',
  content: 'flex-1',
}

/** Texto de ayuda para métricas estimadas (no provienen directamente de la OEC). */
export const AYUDA_ESTIMADO =
  'Estimado a partir de la concentración comercial (sin fuente directa en la OEC).'
