import { useTranslation } from 'react-i18next'
import type { MeleeView } from '../types'

/**
 * "24 jugadores · 12 dupletas" or, in amber, "25 jugadores: no cuadra para dupletas". With an entry
 * fee the admin sees it counted on those who have paid, who are the ones who will play.
 */
export function TeamPlanBanner({ melee }: { melee: MeleeView }) {
  const { t } = useTranslation()
  const { teamPlan, teamSize } = melee
  const sizeName = t(`melee.teamSize.${teamSize}`).toLowerCase()
  const count = teamPlan.players
  const paid = melee.payments !== null

  if (!teamPlan.playable) {
    return (
      <p className="rounded-2xl border-2 border-red-700 bg-red-50 px-4 py-3 text-lg font-semibold text-red-900">
        {paid ? t('players.plan.paid.notEnough', { count }) : t('players.plan.notEnough', { count })}
      </p>
    )
  }
  if (teamPlan.fits) {
    const values = { count, teams: teamPlan.teamCount, size: sizeName }
    return (
      <p className="rounded-2xl border-2 border-green-700 bg-green-50 px-4 py-3 text-lg font-semibold text-green-900">
        {paid ? t('players.plan.paid.fits', values) : t('players.plan.fits', values)}
      </p>
    )
  }
  const odd = Object.entries(teamPlan.teamsBySize)
    .map(([size, count]) => t(`players.plan.teamsOfSize.${size as '2' | '3'}`, { count }))
    .join(t('players.plan.and'))
  return (
    <div role="status" className="rounded-2xl border-2 border-amber-600 bg-amber-50 px-4 py-3 text-lg text-amber-950">
      <p className="font-bold">
        {paid ? t('players.plan.paid.doesNotFit', { count, size: sizeName }) : t('players.plan.doesNotFit', { count, size: sizeName })}
      </p>
      <p>{t('players.plan.oddTeams', { teams: odd })}</p>
    </div>
  )
}
