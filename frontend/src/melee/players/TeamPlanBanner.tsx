import { useTranslation } from 'react-i18next'
import type { MeleeView } from '../types'

/** "24 jugadores · 12 dupletas" or, in amber, "25 jugadores: no cuadra para dupletas". */
export function TeamPlanBanner({ melee }: { melee: MeleeView }) {
  const { t } = useTranslation()
  const { teamPlan, teamSize } = melee
  const sizeName = t(`melee.teamSize.${teamSize}`).toLowerCase()

  if (!teamPlan.playable) {
    return (
      <p className="rounded-2xl border-2 border-red-700 bg-red-50 px-4 py-3 text-lg font-semibold text-red-900">
        {t('players.plan.notEnough', { count: teamPlan.activePlayers })}
      </p>
    )
  }
  if (teamPlan.fits) {
    return (
      <p className="rounded-2xl border-2 border-green-700 bg-green-50 px-4 py-3 text-lg font-semibold text-green-900">
        {t('players.plan.fits', { count: teamPlan.activePlayers, teams: teamPlan.teamCount, size: sizeName })}
      </p>
    )
  }
  const odd = Object.entries(teamPlan.teamsBySize)
    .map(([size, count]) => t(`players.plan.teamsOfSize.${size as '2' | '3'}`, { count }))
    .join(t('players.plan.and'))
  return (
    <div role="status" className="rounded-2xl border-2 border-amber-600 bg-amber-50 px-4 py-3 text-lg text-amber-950">
      <p className="font-bold">{t('players.plan.doesNotFit', { count: teamPlan.activePlayers, size: sizeName })}</p>
      <p>{t('players.plan.oddTeams', { teams: odd })}</p>
    </div>
  )
}
