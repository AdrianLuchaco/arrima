import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Button } from '../ui/Button'
import { Dialog } from '../ui/Dialog'
import { ErrorMessage } from '../ui/ErrorMessage'
import { meleeRequests, useMeleeAction } from './meleeApi'
import { SettingsDialog } from './SettingsDialog'
import { BomboOverlay } from './teams/BomboOverlay'
import type { MeleeView } from './types'
import { useConfirmableAction } from './useConfirmableAction'

interface NextStepCardProps {
  melee: MeleeView
  /** Opens a tab (e.g. the players list to withdraw or add someone). */
  onShowTab: (tab: 'jugadores' | 'equipos' | 'pistas') => void
}

/** The one big action that moves the melee forward, depending on its phase. */
export function NextStepCard({ melee, onShowTab }: NextStepCardProps) {
  // Lives here and not in DrawStep: after the draw the phase changes and DrawStep is replaced.
  const [showDrum, setShowDrum] = useState(false)
  return (
    <>
      {melee.status === 'REGISTRATION' && <DrawStep melee={melee} onShowTab={onShowTab} onDrawn={() => setShowDrum(true)} />}
      {melee.status === 'TEAMS' && <ScheduleStep melee={melee} onGenerated={() => onShowTab('pistas')} />}
      {melee.status === 'MATCHES' && <MatchesStep melee={melee} />}
      {melee.status === 'INTERNATIONAL' && <InternationalStep melee={melee} />}
      {showDrum && <BomboOverlay melee={melee} onDone={() => { setShowDrum(false); onShowTab('equipos') }} />}
    </>
  )
}

function StepCard({ children }: { children: React.ReactNode }) {
  return <section className="mb-5 flex flex-col gap-3 rounded-2xl border-2 border-steel-600 bg-white p-4">{children}</section>
}

function DrawStep({ melee, onShowTab, onDrawn }: NextStepCardProps & { onDrawn: () => void }) {
  const { t } = useTranslation()
  const [doesNotFit, setDoesNotFit] = useState(false)
  const [editingSettings, setEditingSettings] = useState(false)
  const draw = useConfirmableAction(
    melee.id,
    (differentTeam: boolean, confirm) => meleeRequests.drawTeams(melee.id, differentTeam, confirm),
    onDrawn,
  )
  const resume = useMeleeAction(melee.id, () => meleeRequests.resumeTeams(melee.id))
  const plan = melee.teamPlan
  const hasPreviousTeams = melee.teams.length > 0

  function startDraw(differentTeam: boolean) {
    setDoesNotFit(false)
    draw.run(differentTeam)
  }

  return (
    <StepCard>
      {hasPreviousTeams && (
        <>
          <p className="text-lg">{t('steps.draw.previousTeams')}</p>
          <Button busy={resume.isPending} onClick={() => resume.mutate(undefined)}>
            {t('steps.draw.resume')}
          </Button>
        </>
      )}
      {!plan.playable ? (
        <p className="text-lg font-semibold text-red-800">{t('steps.draw.notEnough')}</p>
      ) : (
        <Button
          variant={hasPreviousTeams ? 'secondary' : 'accent'}
          className={hasPreviousTeams ? '' : 'min-h-20 text-2xl'}
          busy={draw.isPending}
          onClick={() => (plan.fits ? startDraw(false) : setDoesNotFit(true))}
        >
          {hasPreviousTeams ? t('steps.draw.redraw') : t('steps.draw.button')}
        </Button>
      )}
      <Button variant="ghost" onClick={() => setEditingSettings(true)}>
        {t('settings.button', melee.settings)}
      </Button>
      <ErrorMessage error={draw.error ?? resume.error} />
      {draw.dialog}
      {editingSettings && <SettingsDialog melee={melee} onClose={() => setEditingSettings(false)} />}

      <Dialog open={doesNotFit} onClose={() => setDoesNotFit(false)} title={t('steps.draw.doesNotFitTitle')}>
        <div className="flex flex-col gap-3">
          <p className="text-lg">
            {t('players.plan.doesNotFit', { count: plan.activePlayers, size: t(`melee.teamSize.${melee.teamSize}`).toLowerCase() })}
          </p>
          <Button variant="accent" onClick={() => startDraw(true)}>
            {t('steps.draw.differentTeam', {
              teams: Object.entries(plan.teamsBySize)
                .map(([size, count]) => t(`players.plan.teamsOfSize.${size as '2' | '3'}`, { count }))
                .join(t('players.plan.and')),
            })}
          </Button>
          <Button variant="secondary" onClick={() => { setDoesNotFit(false); onShowTab('jugadores') }}>
            {t('steps.draw.withdrawSomeone')}
          </Button>
          <Button variant="secondary" onClick={() => { setDoesNotFit(false); onShowTab('jugadores') }}>
            {t('steps.draw.addSomeone')}
          </Button>
        </div>
      </Dialog>

    </StepCard>
  )
}

function ScheduleStep({ melee, onGenerated }: { melee: MeleeView; onGenerated: () => void }) {
  const { t } = useTranslation()
  const [editingSettings, setEditingSettings] = useState(false)
  const generate = useConfirmableAction(melee.id, (_: void, confirm) => meleeRequests.generateSchedule(melee.id, confirm), onGenerated)
  const resume = useMeleeAction(melee.id, () => meleeRequests.resumeSchedule(melee.id), onGenerated)
  const tooManyRounds = melee.settings.roundsCount > melee.maxRounds
  const hasPreviousSchedule = melee.rounds.length > 0
  const blocked = melee.teamIssues.teamsWithoutActivePlayers.length > 0

  return (
    <StepCard>
      {hasPreviousSchedule && (
        <>
          <p className="text-lg">{t('steps.schedule.previous')}</p>
          <Button busy={resume.isPending} onClick={() => resume.mutate(undefined)}>
            {t('steps.schedule.resume')}
          </Button>
        </>
      )}
      {tooManyRounds && (
        <p className="text-lg font-semibold text-red-800">
          {t('settings.tooManyRounds', { teams: melee.teams.length, max: melee.maxRounds })}
        </p>
      )}
      <Button
        variant={hasPreviousSchedule ? 'secondary' : 'accent'}
        className={hasPreviousSchedule ? '' : 'min-h-20 text-2xl'}
        disabled={tooManyRounds || blocked}
        busy={generate.isPending}
        onClick={() => generate.run(undefined)}
      >
        {hasPreviousSchedule ? t('steps.schedule.regenerate') : t('steps.schedule.button')}
      </Button>
      <Button variant="ghost" onClick={() => setEditingSettings(true)}>
        {t('settings.button', melee.settings)}
      </Button>
      <ErrorMessage error={generate.error ?? resume.error} />
      {generate.dialog}
      {editingSettings && <SettingsDialog melee={melee} onClose={() => setEditingSettings(false)} />}
    </StepCard>
  )
}

function MatchesStep({ melee }: { melee: MeleeView }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const start = useConfirmableAction(melee.id, (_: void, confirm) => meleeRequests.startInternational(melee.id, confirm),
    () => navigate(`/melees/${melee.id}/internacional`))
  const pending = melee.rounds.flatMap((round) => round.matches).filter((match) => match.winnerTeamId === null).length
  if (pending > 0) return null
  return (
    <StepCard>
      <p className="text-xl font-bold">{t('steps.matches.allDone')}</p>
      <Button variant="accent" className="min-h-20 text-2xl" busy={start.isPending} onClick={() => start.run(undefined)}>
        {t('steps.international.start')}
      </Button>
      <ErrorMessage error={start.error} />
      {start.dialog}
    </StepCard>
  )
}

function InternationalStep({ melee }: { melee: MeleeView }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const complete = melee.international?.complete ?? false
  return (
    <StepCard>
      <p className="text-xl font-bold">{complete ? t('intl.done') : t('steps.international.inProgress')}</p>
      <Button variant={complete ? 'secondary' : 'accent'} className="min-h-16 text-xl" onClick={() => navigate(`/melees/${melee.id}/internacional`)}>
        {t('steps.international.open')}
      </Button>
    </StepCard>
  )
}
