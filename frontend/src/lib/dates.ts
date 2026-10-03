/** "sábado, 4 de octubre de 2026": the melee is named after its date, as on the paper sheet. */
export function formatMeleeDate(isoDate: string): string {
  // Noon, so no time zone can move the date to the previous or next day.
  const date = new Date(`${isoDate}T12:00:00`)
  return new Intl.DateTimeFormat('es-ES', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(date)
}

export function formatShortDate(isoDate: string): string {
  const date = new Date(`${isoDate}T12:00:00`)
  return new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short', year: 'numeric' }).format(date)
}
