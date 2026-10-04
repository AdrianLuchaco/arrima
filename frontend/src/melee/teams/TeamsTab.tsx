import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { participantName, teamNumber, teamPlayers } from '../names'
import { ResultDialog } from '../ResultDialog'
import type { Match, MeleeView } from '../types'
import { SubstitutionsPanel, TeamEditor } from './TeamEditor'

export function TeamsTab({ melee, readOnly = false }: { melee: MeleeView; readOnly?: boolean }) {
  const { t } = useTranslation()
  if (melee.teams.length === 0) {
    return <p className="text-lg text-steel-600">{t('teams.none')}</p>
  }
  if (melee.status === 'TEAMS' && !readOnly) {
    return <TeamEditor melee={melee} />
  }
  if (melee.rounds.length === 0) {
    return <TeamList melee={melee} />
  }
  return (
    <div className="flex flex-col gap-4">
      {!readOnly && melee.status === 'MATCHES' && <SubstitutionsPanel melee={melee} />}
      <ResultsTable melee={melee} readOnly={readOnly} />
    </div>
  )
}

function TeamList({ melee }: { melee: MeleeView }) {
  const { t } = useTranslation()
  return (
    <ul className="grid gap-2 sm:grid-cols-2">
      {melee.teams.map((team) => (
        <li key={team.id} className="rounded-2xl border-2 border-gravel-300 bg-white px-4 py-3">
          <p className="text-xl font-extrabold">{t('teams.teamNumber', { number: team.number })}</p>
          <p className="text-lg">{teamPlayers(melee, team.id)}</p>
        </li>
      ))}
    </ul>
  )
}

/**
 * Team, players, one column per round and total wins. Before the result a cell shows court and
 * rival ("P3 · 14"); after it, X if won and O if lost, as on paper. The bye counts as a win.
 */
function ResultsTable({ melee, readOnly }: { melee: MeleeView; readOnly: boolean }) {
  const { t } = useTranslation()
  const [selected, setSelected] = useState<{ round: number; match: Match } | null>(null)

  return (
    <div className="flex flex-col gap-4">
      <div className="-mx-4 overflow-x-auto px-4 pb-2">
        <table className="w-full border-separate border-spacing-y-1 text-left">
          <thead>
            <tr className="text-base">
              <th scope="col" className="px-2">{t('teams.short')}</th>
              <th scope="col" className="px-2">{t('teams.players')}</th>
              {melee.rounds.map((round) => (
                <th key={round.number} scope="col" className="px-1 text-center">{t('teams.roundShort', { number: round.number })}</th>
              ))}
              <th scope="col" className="px-2 text-center">{t('teams.total')}</th>
            </tr>
          </thead>
          <tbody>
            {melee.teams.map((team) => (
              <tr key={team.id} className="bg-white">
                <th scope="row" className="rounded-l-xl px-2 py-2 text-2xl font-extrabold">{team.number}</th>
                <td className="min-w-36 px-2 py-2 text-lg leading-snug">
                  {team.memberIds.map((id) => participantName(melee, id)).join(' · ')}
                </td>
                {melee.rounds.map((round) => {
                  if (round.byeTeamId === team.id) {
                    return (
                      <td key={round.number} className="px-1 text-center">
                        <span className="inline-flex min-h-12 min-w-14 flex-col items-center justify-center rounded-lg bg-green-100 text-green-900">
                          <span className="text-xl font-extrabold">X</span>
                          <span className="text-xs font-semibold">{t('teams.bye')}</span>
                        </span>
                      </td>
                    )
                  }
                  const match = round.matches.find((candidate) => candidate.teamAId === team.id || candidate.teamBId === team.id)
                  if (!match) return <td key={round.number} />
                  const rival = match.teamAId === team.id ? match.teamBId : match.teamAId
                  const content =
                    match.winnerTeamId === null ? (
                      <span className="text-base">
                        {match.courtNumber ? t('teams.courtShort', { number: match.courtNumber }) : t('teams.waitingShort')}
                        <br />
                        {t('teams.versus', { number: teamNumber(melee, rival) })}
                      </span>
                    ) : match.winnerTeamId === team.id ? (
                      <span className="text-2xl font-extrabold text-green-800">X</span>
                    ) : (
                      <span className="text-2xl font-extrabold text-red-800">O</span>
                    )
                  return (
                    <td key={round.number} className="px-1 text-center">
                      <button
                        type="button"
                        onClick={() => setSelected({ round: round.number, match })}
                        className="inline-flex min-h-12 min-w-14 items-center justify-center rounded-lg border-2 border-gravel-300 px-1"
                      >
                        {content}
                      </button>
                    </td>
                  )
                })}
                <td className="rounded-r-xl px-2 text-center text-2xl font-extrabold">{team.wins}</td>
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
