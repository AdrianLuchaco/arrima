import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { teamNumber } from '../names'
import { ResultDialog } from '../ResultDialog'
import type { Match, MeleeView } from '../types'

/**
 * The court sheet: one column per court, one row per round, as on the club's paper.
 * "Descansa" and "En espera" columns at the end. Wide schedules scroll sideways.
 */
export function CourtsTab({ melee, readOnly = false }: { melee: MeleeView; readOnly?: boolean }) {
  const { t } = useTranslation()
  const [selected, setSelected] = useState<{ round: number; match: Match } | null>(null)

  if (melee.rounds.length === 0) {
    return <p className="text-lg text-steel-600">{t('courts.none')}</p>
  }

  const courts = Array.from({ length: melee.settings.courtCount }, (_, index) => index + 1)
  const hasWaiting = melee.rounds.some((round) => round.matches.some((match) => match.courtNumber === null))
  const hasByes = melee.rounds.some((round) => round.byeTeamId !== null)
  const pending = melee.rounds.flatMap((round) => round.matches).filter((match) => match.winnerTeamId === null).length

  return (
    <div className="flex flex-col gap-4">
      <p className="text-lg font-semibold">{pending === 0 ? t('courts.allDone') : t('courts.pending', { count: pending })}</p>
      <div className="-mx-4 overflow-x-auto px-4 pb-2">
        <table className="border-separate border-spacing-1 text-center">
          <thead>
            <tr>
              <th scope="col" className="sticky left-0 z-10 bg-gravel-50 px-2 text-left text-base">{t('courts.round')}</th>
              {courts.map((court) => (
                <th key={court} scope="col" className="min-w-20 px-1 text-base">{t('courts.court', { number: court })}</th>
              ))}
              {hasByes && <th scope="col" className="min-w-20 px-1 text-base">{t('courts.bye')}</th>}
              {hasWaiting && <th scope="col" className="min-w-24 px-1 text-base">{t('courts.waiting')}</th>}
            </tr>
          </thead>
          <tbody>
            {melee.rounds.map((round) => (
              <tr key={round.number}>
                <th scope="row" className="sticky left-0 z-10 whitespace-nowrap bg-gravel-50 px-2 text-left text-lg">{t('courts.roundNumber', { number: round.number })}</th>
                {courts.map((court) => (
                  <td key={court} className="align-top">
                    {round.matches.filter((match) => match.courtNumber === court).map((match) => (
                      <MatchCell key={match.id} melee={melee} match={match} readOnly={readOnly} onOpen={() => setSelected({ round: round.number, match })} />
                    ))}
                  </td>
                ))}
                {hasByes && (
                  <td className="rounded-xl bg-green-50 text-xl font-bold text-green-900">
                    {round.byeTeamId !== null && teamNumber(melee, round.byeTeamId)}
                  </td>
                )}
                {hasWaiting && (
                  <td className="align-top">
                    {round.matches.filter((match) => match.courtNumber === null).map((match) => (
                      <MatchCell key={match.id} melee={melee} match={match} readOnly={readOnly} onOpen={() => setSelected({ round: round.number, match })} />
                    ))}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {selected && (
        <ResultDialog melee={melee} roundNumber={selected.round} match={selected.match} readOnly={readOnly} onClose={() => setSelected(null)} />
      )}
    </div>
  )
}

function MatchCell({ melee, match, readOnly, onOpen }: { melee: MeleeView; match: Match; readOnly: boolean; onOpen: () => void }) {
  const { t } = useTranslation()
  const a = teamNumber(melee, match.teamAId)
  const b = teamNumber(melee, match.teamBId)
  const decided = match.winnerTeamId !== null
  const number = (teamId: number, value: number) =>
    match.winnerTeamId === teamId ? <strong className="underline decoration-4 underline-offset-4">{value}</strong> : <span>{value}</span>
  return (
    <button
      type="button"
      onClick={onOpen}
      aria-label={t('courts.matchLabel', { a, b })}
      className={`my-0.5 flex min-h-14 w-full items-center justify-center gap-1 rounded-xl border-2 px-2 text-xl ${decided ? 'border-green-700 bg-green-50' : 'border-steel-400 bg-white'} ${readOnly ? 'cursor-default' : ''}`}
    >
      {number(match.teamAId, a)}
      <span aria-hidden="true">–</span>
      {number(match.teamBId, b)}
    </button>
  )
}
