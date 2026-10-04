import { useTranslation } from 'react-i18next'
import { teamNumber, teamPlayers } from '../melee/names'
import type { IntlGroup, MeleeView, Ranked } from '../melee/types'
import { groupTitle } from './labels'

/**
 * The scoreboard of la Internacional: each group with its teams and points, and the final list.
 * Read-only; the admin screen adds the turn and the field on top.
 */
export function IntlBoard({ melee, onTeam }: { melee: MeleeView; onTeam?: (groupId: number, teamId: number) => void }) {
  const { t } = useTranslation()
  const intl = melee.international
  if (!intl) return <p className="text-lg text-steel-600">{t('intl.notStarted')}</p>

  return (
    <div className="flex flex-col gap-5">
      {intl.complete && <FinalList melee={melee} ranking={intl.finalRanking} />}
      {intl.groups.length === 0 && <p className="text-lg">{t('intl.nobodyPlays')}</p>}
      {intl.assured.length > 0 && !intl.complete && (
        <section className="rounded-2xl border-2 border-gravel-300 bg-white p-4">
          <h3 className="mb-2 text-xl font-bold">{t('intl.assured')}</h3>
          <ul className="flex flex-col gap-1 text-lg">
            {intl.assured.map((ranked) => (
              <li key={ranked.teamId}>
                <strong>{t('intl.prize', { position: ranked.position })}</strong> · {t('teams.teamNumber', { number: teamNumber(melee, ranked.teamId) })} ({teamPlayers(melee, ranked.teamId)})
              </li>
            ))}
          </ul>
        </section>
      )}
      {[...intl.groups].sort((a, b) => a.playOrder - b.playOrder).map((group) => (
        <GroupCard key={group.id} melee={melee} group={group} onTeam={onTeam} />
      ))}
    </div>
  )
}

function GroupCard({ melee, group, onTeam }: { melee: MeleeView; group: IntlGroup; onTeam?: (groupId: number, teamId: number) => void }) {
  const { t } = useTranslation()
  const applied = group.rounds.filter((round) => !round.obsolete)
  return (
    <section className="rounded-2xl border-2 border-gravel-300 bg-white p-4">
      <h3 className="text-xl font-bold">{groupTitle(t, group)}</h3>
      <p className="mb-3 text-base text-steel-600">{t(`intl.groupStatus.${group.status}`)}</p>
      {group.obsoletePlayed && (
        <p role="alert" className="mb-3 rounded-xl border-2 border-amber-600 bg-amber-50 px-3 py-2 text-lg text-amber-950">
          {t('intl.obsoleteWarning')}
        </p>
      )}
      <ol className="flex flex-col gap-2">
        {group.order.map((teamId, index) => {
          const main = applied[0]?.teams.find((team) => team.teamId === teamId)
          const tieBreaks = applied.slice(1).flatMap((round) => round.teams.filter((team) => team.teamId === teamId))
          const content = (
            <>
              <span className="w-10 shrink-0 text-xl font-extrabold text-steel-600">{index + 1}.</span>
              <span className="min-w-0 flex-1 text-left">
                <span className="block text-lg font-bold">{t('teams.teamNumber', { number: teamNumber(melee, teamId) })}</span>
                <span className="block text-base">{teamPlayers(melee, teamId)}</span>
              </span>
              <span className="text-right">
                <span className="block text-2xl font-extrabold">{main?.points ?? 0}</span>
                {tieBreaks.map((team, i) => (
                  <span key={i} className="block text-sm text-steel-600">{t('intl.tieBreakPoints', { points: team.points })}</span>
                ))}
              </span>
            </>
          )
          return (
            <li key={teamId}>
              {onTeam ? (
                <button type="button" onClick={() => onTeam(group.id, teamId)} className="flex w-full items-center gap-3 rounded-xl border-2 border-gravel-300 px-3 py-2">
                  {content}
                </button>
              ) : (
                <div className="flex items-center gap-3 rounded-xl border-2 border-gravel-100 px-3 py-2">{content}</div>
              )}
            </li>
          )
        })}
      </ol>
    </section>
  )
}

function FinalList({ melee, ranking }: { melee: MeleeView; ranking: Ranked[] }) {
  const { t } = useTranslation()
  return (
    <section className="rounded-2xl border-4 border-jack-500 bg-white p-4">
      <h3 className="mb-3 text-2xl font-extrabold">{t('intl.finalTitle')}</h3>
      <ol className="flex flex-col gap-2">
        {ranking.map((ranked) => (
          <li key={ranked.teamId} className="flex items-center gap-3 text-lg">
            <span className="w-14 shrink-0 text-2xl font-extrabold">{t('intl.prizeShort', { position: ranked.position })}</span>
            <span className="min-w-0 flex-1">
              <strong>{t('teams.teamNumber', { number: teamNumber(melee, ranked.teamId) })}</strong> · {teamPlayers(melee, ranked.teamId)}
            </span>
            <span className="text-right">
              {ranked.points !== null ? t('intl.points', { count: ranked.points }) : t('intl.direct')}
              {ranked.tieBreak && <span className="block text-sm text-steel-600">{t('intl.afterTieBreak')}</span>}
            </span>
          </li>
        ))}
      </ol>
    </section>
  )
}
