import { useTranslation } from 'react-i18next'
import { enqueue } from '../offline/outbox'
import { Button } from '../ui/Button'
import { Dialog } from '../ui/Dialog'
import { ErrorMessage } from '../ui/ErrorMessage'
import { meleeRequests, useMeleeAction } from './meleeApi'
import { teamNumber, teamPlayers } from './names'
import type { Match, MeleeView } from './types'

interface ResultDialogProps {
  melee: MeleeView
  roundNumber: number
  match: Match
  onClose: () => void
  readOnly?: boolean
}

/**
 * Two big buttons with the teams' names: one tap marks the X of one and the O of the other.
 * The tap goes to the offline queue, so it is never lost even without signal.
 */
export function ResultDialog({ melee, roundNumber, match, onClose, readOnly = false }: ResultDialogProps) {
  const { t } = useTranslation()
  const editable = !readOnly && melee.status === 'MATCHES'

  function choose(winnerTeamId: number | null) {
    enqueue({
      key: `winner:${match.id}`,
      meleeId: melee.id,
      path: `/api/melees/${melee.id}/schedule/matchups/${match.id}/winner`,
      body: { winnerTeamId },
      overlay: { kind: 'winner', matchId: match.id, winnerTeamId },
    })
    onClose()
  }

  const title = match.courtNumber
    ? t('result.titleCourt', { round: roundNumber, court: match.courtNumber })
    : t('result.titleWaiting', { round: roundNumber })

  return (
    <Dialog open onClose={onClose} title={title}>
      <div className="flex flex-col gap-4">
        <p className="text-lg">{editable ? t('result.question') : t('result.readOnly')}</p>
        {[match.teamAId, match.teamBId].map((teamId) => {
          const won = match.winnerTeamId === teamId
          return (
            <button
              key={teamId}
              type="button"
              disabled={!editable}
              onClick={() => choose(teamId)}
              className={`flex min-h-24 flex-col items-start justify-center rounded-2xl border-4 px-4 py-3 text-left ${won ? 'border-green-700 bg-green-50' : 'border-steel-400 bg-white'} disabled:opacity-80`}
            >
              <span className="text-2xl font-extrabold">
                {t('teams.teamNumber', { number: teamNumber(melee, teamId) })}
                {won && <span className="ml-2 rounded-lg bg-green-700 px-2 py-0.5 text-base text-white">{t('result.won')}</span>}
              </span>
              <span className="text-lg">{teamPlayers(melee, teamId)}</span>
            </button>
          )
        })}
        {editable && match.winnerTeamId !== null && (
          <Button variant="ghost" onClick={() => choose(null)}>
            {t('result.clear')}
          </Button>
        )}
        {editable && match.winnerTeamId === null && match.courtNumber === null && (
          <CourtPicker melee={melee} roundNumber={roundNumber} match={match} />
        )}
      </div>
    </Dialog>
  )
}

/** For a waiting match: courts with an unfinished match of the same round are taken. */
function CourtPicker({ melee, roundNumber, match }: { melee: MeleeView; roundNumber: number; match: Match }) {
  const { t } = useTranslation()
  const assign = useMeleeAction(melee.id, (court: number) => meleeRequests.assignCourt(melee.id, match.id, court))
  const round = melee.rounds.find((candidate) => candidate.number === roundNumber)
  const busy = new Set(
    round?.matches.filter((other) => other.id !== match.id && other.winnerTeamId === null && other.courtNumber !== null)
      .map((other) => other.courtNumber),
  )
  const courts = Array.from({ length: melee.settings.courtCount }, (_, index) => index + 1)

  return (
    <div className="border-t-2 border-gravel-300 pt-4">
      <p className="mb-2 text-lg font-semibold">{t('result.assignCourt')}</p>
      <div className="grid grid-cols-4 gap-2 sm:grid-cols-6">
        {courts.map((court) => (
          <button
            key={court}
            type="button"
            disabled={busy.has(court) || assign.isPending}
            onClick={() => assign.mutate(court)}
            className="min-h-14 rounded-xl border-2 border-steel-400 bg-white text-xl font-bold disabled:border-gravel-300 disabled:bg-gravel-100 disabled:text-steel-400"
          >
            {court}
          </button>
        ))}
      </div>
      <div className="mt-2">
        <ErrorMessage error={assign.error} />
      </div>
    </div>
  )
}
