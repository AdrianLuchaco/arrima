import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { useMeleeLive } from '../live/useMeleeLive'
import { meleeKey, useMelee } from '../melee/meleeApi'
import { teamNumber, teamPlayers } from '../melee/names'
import type { MeleeView, ThrowOutcome, Turn } from '../melee/types'
import { useOutbox } from '../offline/useOutbox'
import { withPending } from '../offline/withPending'
import { ConfirmDialog } from '../ui/ConfirmDialog'
import { ErrorMessage } from '../ui/ErrorMessage'
import { Field } from './Field'
import { IntlBoard } from './IntlBoard'
import { localTurn, pointsFor, recordThrow } from './intl'
import { groupTitle, positionLabel } from './labels'
import { TeamBallsDialog } from './TeamBallsDialog'

/**
 * "Iniciar la Internacional": the app says whose turn it is, in big letters, and the admin taps where
 * the ball ended up. Every tap is confirmed, then goes to the offline queue; after the third ball the
 * next team comes up by itself.
 */
export function InternationalPage() {
  const { t } = useTranslation()
  const meleeId = Number(useParams().meleeId)
  const { data, error } = useMelee(meleeId)
  const { pending } = useOutbox()
  const queryClient = useQueryClient()
  useMeleeLive(data?.publicCode, () => void queryClient.invalidateQueries({ queryKey: meleeKey(meleeId) }))
  const [teamDetail, setTeamDetail] = useState<{ groupId: number; teamId: number } | null>(null)

  if (error) return <ErrorMessage error={error} />
  if (!data) return null
  const melee = withPending(data, pending)
  const intl = melee.international

  const throwsPending = pending.some((action) => action.meleeId === melee.id && action.overlay.kind === 'throw')
  const turn = throwsPending ? localTurn(melee) : (intl?.turn ?? null)
  const editable = melee.status === 'INTERNATIONAL'

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-baseline justify-between gap-3">
        <h1 className="text-2xl font-extrabold">{t('melee.status.INTERNATIONAL')}</h1>
        <Link to={`/melees/${melee.id}`} className="text-base font-semibold underline underline-offset-4">
          {t('intl.backToMelee')}
        </Link>
      </div>

      {editable && turn && 'awaitingServer' in turn && (
        <p role="status" className="rounded-2xl border-2 border-amber-600 bg-amber-50 p-4 text-xl font-semibold text-amber-950">
          {t('intl.awaitingServer')}
        </p>
      )}
      {editable && turn && !('awaitingServer' in turn) && <TurnPanel melee={melee} turn={turn} />}
      {intl && intl.complete && <p className="rounded-2xl bg-green-50 p-4 text-xl font-bold text-green-900">{t('intl.done')}</p>}

      <IntlBoard melee={melee} onTeam={(groupId, teamId) => setTeamDetail({ groupId, teamId })} />

      {teamDetail && (
        <TeamBallsDialog melee={melee} groupId={teamDetail.groupId} teamId={teamDetail.teamId} editable={editable}
          onClose={() => setTeamDetail(null)} />
      )}
    </div>
  )
}

function TurnPanel({ melee, turn }: { melee: MeleeView; turn: Turn }) {
  const { t } = useTranslation()
  const [picked, setPicked] = useState<ThrowOutcome | null>(null)
  const group = melee.international!.groups.find((candidate) => candidate.id === turn.groupId)
  const round = group?.rounds.find((candidate) => candidate.id === turn.roundId)
  const teamSize = melee.teams.find((team) => team.id === turn.teamId)?.memberIds.length ?? 2
  const roundPoints = round?.teams.find((team) => team.teamId === turn.teamId)?.points ?? 0

  function confirm() {
    if (!picked) return
    recordThrow(melee, turn.roundId, turn.teamId, turn.kind, turn.ballNumber, picked)
    setPicked(null)
  }

  return (
    <section key={`${turn.roundId}-${turn.teamId}-${turn.kind}-${turn.ballNumber}`} className="arrima-reveal flex flex-col gap-4">
      <div className="rounded-3xl bg-steel-800 px-4 py-3 text-gravel-50">
        <p className="text-base">
          {group && groupTitle(t, group)}
          {round && round.number > 1 && <span className="ml-2 rounded-lg bg-jack-500 px-2 py-0.5 font-bold text-white">{t('intl.tieBreak')}</span>}
        </p>
        <p className="text-2xl leading-tight">
          <strong className="text-4xl font-extrabold">{t('teams.teamNumber', { number: teamNumber(melee, turn.teamId) })}</strong>{' '}
          · {teamPlayers(melee, turn.teamId)}
        </p>
        <p className="mt-1 text-2xl font-extrabold text-jack-500 uppercase sm:text-3xl">
          {turn.kind === 'POINTING' ? t('intl.points_action') : t('intl.shoots_action')} · {positionLabel(t, teamSize, turn.position)}
        </p>
        <p className="text-lg">
          {t('intl.ballOf', { ball: turn.ballNumber })} · {t('intl.roundPoints', { count: roundPoints })}
        </p>
      </div>
      <Field key={`${turn.roundId}-${turn.teamId}-${turn.kind}-${turn.ballNumber}`} kind={turn.kind} scoring={melee.scoring} onPick={setPicked} />
      <ConfirmDialog
        open={picked !== null}
        title={picked ? t('intl.confirmTitle', { outcome: t(`intl.outcome.${picked}`) }) : ''}
        confirmLabel={t('common.yes')}
        onCancel={() => setPicked(null)}
        onConfirm={confirm}
      >
        {picked && <p className="text-2xl font-bold">{t('intl.points', { count: pointsFor(melee.scoring, picked) })}</p>}
      </ConfirmDialog>
    </section>
  )
}
