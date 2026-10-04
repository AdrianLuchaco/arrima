import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { meleeRequests, useMeleeAction } from '../meleeApi'
import { participantName } from '../names'
import type { MeleeView } from '../types'
import { Button } from '../../ui/Button'
import { ConfirmDialog } from '../../ui/ConfirmDialog'
import { Dialog } from '../../ui/Dialog'
import { ErrorMessage } from '../../ui/ErrorMessage'

/**
 * Before the matches: the drawn teams, with two corrections by hand — exchange two players, and
 * replace someone who has left (withdrawn) with a player who is in no team.
 */
export function TeamEditor({ melee }: { melee: MeleeView }) {
  const { t } = useTranslation()
  const [swapping, setSwapping] = useState(false)
  const [first, setFirst] = useState<number | null>(null)
  const [second, setSecond] = useState<number | null>(null)
  const [replacing, setReplacing] = useState<number | null>(null)
  const swap = useMeleeAction(melee.id, () => meleeRequests.swapPlayers(melee.id, first!, second!))
  const withdrawn = new Set(melee.teamIssues.withdrawnMembers)
  const teamOf = (playerId: number) => melee.teams.find((team) => team.memberIds.includes(playerId))?.number

  function pick(playerId: number) {
    if (!swapping) return
    if (first === null) setFirst(playerId)
    else if (first === playerId) setFirst(null)
    else if (teamOf(first) !== teamOf(playerId)) setSecond(playerId)
  }

  function endSwap() {
    setSwapping(false)
    setFirst(null)
    setSecond(null)
  }

  return (
    <div className="flex flex-col gap-4">
      <TeamIssuesNotice melee={melee} />
      <Button variant={swapping ? 'primary' : 'secondary'} onClick={() => (swapping ? endSwap() : setSwapping(true))}>
        {swapping ? t('teams.swap.stop') : t('teams.swap.start')}
      </Button>
      {swapping && <p className="text-lg font-semibold">{first === null ? t('teams.swap.pickFirst') : t('teams.swap.pickSecond')}</p>}

      <ul className="grid gap-2 sm:grid-cols-2">
        {melee.teams.map((team) => (
          <li key={team.id} className="rounded-2xl border-2 border-gravel-300 bg-white p-3">
            <p className="mb-2 text-xl font-extrabold">{t('teams.teamNumber', { number: team.number })}</p>
            <ul className="flex flex-col gap-2">
              {team.memberIds.map((playerId) => (
                <li key={playerId} className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => pick(playerId)}
                    disabled={!swapping}
                    aria-pressed={first === playerId}
                    className={`min-h-12 flex-1 rounded-xl px-3 text-left text-lg ${first === playerId ? 'bg-jack-500 text-white' : swapping ? 'border-2 border-steel-400' : ''} ${withdrawn.has(playerId) ? 'text-red-800 line-through' : ''}`}
                  >
                    {participantName(melee, playerId)}
                  </button>
                  {withdrawn.has(playerId) && (
                    <Button variant="secondary" className="min-h-12" onClick={() => setReplacing(playerId)}>
                      {t('teams.substitute.button')}
                    </Button>
                  )}
                </li>
              ))}
            </ul>
          </li>
        ))}
      </ul>

      <ConfirmDialog
        open={second !== null}
        title={t('teams.swap.confirmTitle')}
        confirmLabel={t('teams.swap.confirm')}
        busy={swap.isPending}
        error={swap.error}
        onCancel={() => setSecond(null)}
        onConfirm={() => swap.mutate(undefined, { onSuccess: endSwap })}
      >
        {first !== null && second !== null && (
          <p>
            {t('teams.swap.explanation', {
              first: participantName(melee, first),
              firstTeam: teamOf(first),
              second: participantName(melee, second),
              secondTeam: teamOf(second),
            })}
          </p>
        )}
      </ConfirmDialog>

      {replacing !== null && <SubstituteDialog melee={melee} leaving={replacing} onClose={() => setReplacing(null)} />}
    </div>
  )
}

export function TeamIssuesNotice({ melee }: { melee: MeleeView }) {
  const { t } = useTranslation()
  const { unassignedPlayers, withdrawnMembers, teamsWithoutActivePlayers } = melee.teamIssues
  if (unassignedPlayers.length + withdrawnMembers.length + teamsWithoutActivePlayers.length === 0) return null
  return (
    <div role="status" className="rounded-2xl border-2 border-amber-600 bg-amber-50 px-4 py-3 text-lg text-amber-950">
      {unassignedPlayers.length > 0 && (
        <p>{t('teams.issues.unassigned', { names: unassignedPlayers.map((id) => participantName(melee, id)).join(', ') })}</p>
      )}
      {withdrawnMembers.length > 0 && (
        <p>{t('teams.issues.withdrawn', { names: withdrawnMembers.map((id) => participantName(melee, id)).join(', ') })}</p>
      )}
      {teamsWithoutActivePlayers.length > 0 && (
        <p className="font-bold">{t('teams.issues.empty', { teams: teamsWithoutActivePlayers.join(', ') })}</p>
      )}
    </div>
  )
}

export function SubstituteDialog({ melee, leaving, onClose }: { melee: MeleeView; leaving: number; onClose: () => void }) {
  const { t } = useTranslation()
  const substitute = useMeleeAction(melee.id, (joining: number) => meleeRequests.substitute(melee.id, leaving, joining))
  const candidates = melee.teamIssues.unassignedPlayers
  return (
    <Dialog open onClose={onClose} title={t('teams.substitute.title', { name: participantName(melee, leaving) })}>
      <div className="flex flex-col gap-3">
        {candidates.length === 0 ? (
          <p className="text-lg">{t('teams.substitute.nobody')}</p>
        ) : (
          candidates.map((id) => (
            <Button key={id} variant="secondary" busy={substitute.isPending} onClick={() => substitute.mutate(id, { onSuccess: onClose })}>
              {participantName(melee, id)}
            </Button>
          ))
        )}
        <ErrorMessage error={substitute.error} />
      </div>
    </Dialog>
  )
}

/** During the matches: withdrawn players who are still in a team can be replaced. */
export function SubstitutionsPanel({ melee }: { melee: MeleeView }) {
  const { t } = useTranslation()
  const [replacing, setReplacing] = useState<number | null>(null)
  if (melee.teamIssues.withdrawnMembers.length === 0) return null
  return (
    <div className="flex flex-col gap-2">
      <TeamIssuesNotice melee={melee} />
      {melee.teamIssues.withdrawnMembers.map((playerId) => (
        <Button key={playerId} variant="secondary" onClick={() => setReplacing(playerId)}>
          {t('teams.substitute.forPlayer', { name: participantName(melee, playerId) })}
        </Button>
      ))}
      {replacing !== null && <SubstituteDialog melee={melee} leaving={replacing} onClose={() => setReplacing(null)} />}
    </div>
  )
}
