import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { nextMatchOf } from '../live/nextMatch'
import { teamNumber, teamPlayers } from '../melee/names'
import type { MeleeView } from '../melee/types'

const storageKey = (code: string) => `arrima.myTeam.${code}`

function readChoice(code: string): number | null {
  try {
    const stored = localStorage.getItem(storageKey(code))
    return stored ? Number(stored) : null
  } catch {
    return null
  }
}

/**
 * "¿Cuál es tu equipo?": chosen once (remembered on this phone), then the next match is always
 * highlighted: "Tu próxima partida: Pista 3 contra el Equipo 4 (Paqui y Pepe)".
 */
export function MyTeamCard({ melee }: { melee: MeleeView }) {
  const { t } = useTranslation()
  const [chosen, setChosen] = useState(() => readChoice(melee.publicCode))
  const team = melee.teams.find((candidate) => candidate.id === chosen)

  function choose(teamId: number | null) {
    setChosen(teamId)
    try {
      if (teamId === null) localStorage.removeItem(storageKey(melee.publicCode))
      else localStorage.setItem(storageKey(melee.publicCode), String(teamId))
    } catch {
      // Without storage the choice simply lasts while the page is open.
    }
  }

  if (melee.teams.length === 0) return null

  if (!team) {
    return (
      <section className="mb-5 rounded-2xl border-2 border-steel-600 bg-white p-4">
        <label htmlFor="my-team" className="mb-2 block text-xl font-bold">{t('myTeam.question')}</label>
        <select
          id="my-team"
          value=""
          onChange={(event) => choose(Number(event.target.value))}
          className="h-14 w-full rounded-xl border-2 border-steel-400 bg-white px-3 text-lg"
        >
          <option value="" disabled>{t('myTeam.choose')}</option>
          {melee.teams.map((candidate) => (
            <option key={candidate.id} value={candidate.id}>
              {t('teams.teamNumber', { number: candidate.number })} · {teamPlayers(melee, candidate.id)}
            </option>
          ))}
        </select>
      </section>
    )
  }

  const next = nextMatchOf(melee, team.id)
  return (
    <section className="mb-5 rounded-2xl border-4 border-jack-500 bg-white p-4">
      <p className="text-lg">
        {t('teams.teamNumber', { number: team.number })} · {teamPlayers(melee, team.id)} ·{' '}
        <strong>{t('myTeam.wins', { count: team.wins })}</strong>
      </p>
      <p className="mt-1 text-2xl font-extrabold">
        {next.kind === 'match' &&
          (next.match.courtNumber
            ? t('myTeam.nextOnCourt', { round: next.round, court: next.match.courtNumber, rival: teamNumber(melee, next.rivalId) })
            : t('myTeam.nextWaiting', { round: next.round, rival: teamNumber(melee, next.rivalId) }))}
        {next.kind === 'bye' && t('myTeam.resting', { round: next.round })}
        {next.kind === 'finished' && t('myTeam.finished')}
        {next.kind === 'notScheduled' && t('myTeam.notScheduled')}
      </p>
      {next.kind === 'match' && <p className="text-lg">{teamPlayers(melee, next.rivalId)}</p>}
      <button type="button" onClick={() => choose(null)} className="mt-3 text-base font-semibold underline underline-offset-4">
        {t('myTeam.change')}
      </button>
    </section>
  )
}
