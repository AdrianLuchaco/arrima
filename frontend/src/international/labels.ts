import type { TFunction } from 'i18next'
import { ordinalBeforeNoun } from '../lib/ordinal'
import type { IntlGroup, PlayerPosition } from '../melee/types'

/** "Arrimador" in a doublette, "Punta" in a triplette; "Medio" only exists in triplettes. */
export function positionLabel(t: TFunction, teamSize: number, position: PlayerPosition): string {
  if (position === 'POINTER') return teamSize === 3 ? t('intl.position.lead') : t('intl.position.pointer')
  if (position === 'MIDDLE') return t('intl.position.middle')
  return t('intl.position.shooter')
}

/** "Grupo de 2 victorias · premios 4.º a 5.º" */
export function groupTitle(t: TFunction, group: IntlGroup): string {
  const prizes =
    group.bestPosition === group.worstPosition
      ? t('intl.prizeSingle', { ordinal: ordinalBeforeNoun(group.bestPosition) })
      : t('intl.prizeRange', { best: group.bestPosition, worst: group.worstPosition })
  return t('intl.groupTitle', { count: group.wins, prizes })
}
