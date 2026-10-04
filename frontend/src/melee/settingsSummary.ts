import type { TFunction } from 'i18next'
import { formatEuros } from '../lib/money'
import type { MeleeSettings } from './types'

/** "3 partidas · 5 premios · 8 pistas", plus "· cuota de 5 €" when there is an entry fee. */
export function settingsSummary(t: TFunction, settings: MeleeSettings): string {
  const summary = t('melee.summary', settings)
  return settings.entryFeeCents > 0 ? `${summary} · ${t('melee.fee', { amount: formatEuros(settings.entryFeeCents) })}` : summary
}
