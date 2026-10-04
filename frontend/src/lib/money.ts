/** Same limit as the backend (MeleeSettings.MAX_ENTRY_FEE_CENTS): 100 €. */
export const MAX_ENTRY_FEE_CENTS = 10_000

/**
 * Amounts travel in cents (2,50 € is 250), so they are always exact, and are shown the Spanish way:
 * "170 €" when there are no cents, "2,50 €" when there are.
 */
export function formatEuros(cents: number): string {
  return new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: cents % 100 === 0 ? 0 : 2,
    maximumFractionDigits: 2,
  }).format(cents / 100)
}

/** The amount as typed in a field ("5", "2,5", "2.50 €"), in cents; null if it is not an amount. */
export function parseEuros(text: string): number | null {
  const cleaned = text.replace(/€/g, '').replace(/\s/g, '').replace(',', '.')
  if (!/^\d+(\.\d{0,2})?$/.test(cleaned)) return null
  return Math.round(Number(cleaned) * 100)
}

/** For an input: "5,00", "2,50". */
export function eurosForInput(cents: number): string {
  return (cents / 100).toFixed(2).replace('.', ',')
}
