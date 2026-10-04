import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { teamNumber, teamPlayers } from '../melee/names'
import type { MeleeView, ThrowKind, ThrowOutcome } from '../melee/types'
import { ConfirmDialog } from '../ui/ConfirmDialog'
import { Dialog } from '../ui/Dialog'
import { OutcomeButtons } from './Field'
import { pointsFor, POINTING_OUTCOMES, positionOf, recordThrow, SHOOTING_OUTCOMES } from './intl'
import { positionLabel } from './labels'

interface Props {
  melee: MeleeView
  groupId: number
  teamId: number
  editable: boolean
  onClose: () => void
}

/**
 * A team complains: here are its six balls in every round it played, and any of them can be
 * corrected. The ranking is recalculated by the server; if a tie-break already played no longer
 * applies, the group shows a warning.
 */
export function TeamBallsDialog({ melee, groupId, teamId, editable, onClose }: Props) {
  const { t } = useTranslation()
  const [editing, setEditing] = useState<{ roundId: number; kind: ThrowKind; ballNumber: number } | null>(null)
  const [picked, setPicked] = useState<ThrowOutcome | null>(null)
  const group = melee.international?.groups.find((candidate) => candidate.id === groupId)
  const teamSize = melee.teams.find((team) => team.id === teamId)?.memberIds.length ?? 2
  const rounds = group?.rounds.filter((round) => round.teams.some((team) => team.teamId === teamId)) ?? []

  return (
    <Dialog open onClose={onClose} title={t('intl.teamBalls', { number: teamNumber(melee, teamId) })}>
      <div className="flex flex-col gap-5">
        <p className="text-lg">{teamPlayers(melee, teamId)}</p>
        {rounds.map((round) => {
          const team = round.teams.find((candidate) => candidate.teamId === teamId)!
          return (
            <section key={round.id}>
              <h3 className="mb-2 text-xl font-bold">
                {round.number === 1 ? t('intl.mainRound') : t('intl.tieBreakRound', { number: round.number - 1 })}
                {round.obsolete && <span className="ml-2 text-base text-amber-800">({t('intl.obsolete')})</span>}
                <span className="ml-2 text-steel-600">· {t('intl.points', { count: team.points })}</span>
              </h3>
              <ul className="grid gap-2">
                {(['POINTING', 'SHOOTING'] as const).flatMap((kind) =>
                  [1, 2, 3].map((ballNumber) => {
                    const ball = team.balls.find((candidate) => candidate.kind === kind && candidate.ballNumber === ballNumber)
                    const label = `${kind === 'POINTING' ? t('intl.points_action') : t('intl.shoots_action')} ${ballNumber} · ${positionLabel(t, teamSize, positionOf(teamSize, kind, ballNumber))}`
                    return (
                      <li key={`${kind}${ballNumber}`}>
                        <button
                          type="button"
                          disabled={!editable || !ball}
                          onClick={() => setEditing({ roundId: round.id, kind, ballNumber })}
                          className="flex min-h-14 w-full items-center justify-between gap-2 rounded-xl border-2 border-gravel-300 bg-white px-3 text-left text-lg"
                        >
                          <span>{label}</span>
                          <span className="font-bold">
                            {ball ? `${t(`intl.outcome.${ball.outcome}`)} (${ball.points})` : '—'}
                            {ball?.corrected && <span className="ml-1 text-sm text-amber-800">{t('intl.corrected')}</span>}
                          </span>
                        </button>
                      </li>
                    )
                  }),
                )}
              </ul>
            </section>
          )
        })}

        {editing && !picked && (
          <section className="border-t-2 border-gravel-300 pt-4">
            <h3 className="mb-2 text-xl font-bold">{t('intl.correctTo')}</h3>
            <OutcomeButtons outcomes={editing.kind === 'POINTING' ? POINTING_OUTCOMES : SHOOTING_OUTCOMES} scoring={melee.scoring} onPick={setPicked} />
          </section>
        )}
        <ConfirmDialog
          open={picked !== null}
          title={picked ? t('intl.confirmTitle', { outcome: t(`intl.outcome.${picked}`) }) : ''}
          confirmLabel={t('common.yes')}
          onCancel={() => setPicked(null)}
          onConfirm={() => {
            if (editing && picked) recordThrow(melee, editing.roundId, teamId, editing.kind, editing.ballNumber, picked)
            setPicked(null)
            setEditing(null)
          }}
        >
          {picked && <p className="text-2xl font-bold">{t('intl.points', { count: pointsFor(melee.scoring, picked) })}</p>}
        </ConfirmDialog>
      </div>
    </Dialog>
  )
}
