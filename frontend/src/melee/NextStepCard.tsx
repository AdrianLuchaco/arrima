import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Button } from '../ui/Button'
import { ConfirmDialog } from '../ui/ConfirmDialog'
import { Dialog } from '../ui/Dialog'
import { ErrorMessage } from '../ui/ErrorMessage'
import { meleeRequests, useMeleeAction } from './meleeApi'
import { unmarkedPeople } from './payments/payments'
import { SettingsDialog } from './SettingsDialog'
import { settingsSummary } from './settingsSummary'
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
      {melee.status === 'PRIZES' && <PrizesStep melee={melee} />}
      {showDrum && <BomboOverlay melee={melee} onDone={() => { setShowDrum(false); onShowTab('equipos') }} />}
    </>
  )
}

function StepCard({ children }: { children: React.ReactNode }) {
  return <section className="mb-5 flex flex-col gap-3 rounded-2xl border-2 border-steel-600 bg-white p-4">{children}</section>
}

interface DrawChoice {
  differentTeam: boolean
  unmarkedDidNotPay: boolean
}

function DrawStep({ melee, onShowTab, onDrawn }: NextStepCardProps & { onDrawn: () => void }) {
  const { t } = useTranslation()
  const [askingUnmarked, setAskingUnmarked] = useState(false)
  const [doesNotFit, setDoesNotFit] = useState(false)
  // Already confirmed that whoever is unmarked did not pay, for the draw that follows.
  const [unmarkedDidNotPay, setUnmarkedDidNotPay] = useState(false)
  const [editingSettings, setEditingSettings] = useState(false)
  const draw = useConfirmableAction(
    melee.id,
    (choice: DrawChoice, confirm) => meleeRequests.drawTeams(melee.id, choice.differentTeam, choice.unmarkedDidNotPay, confirm),
    onDrawn,
  )
  const resume = useMeleeAction(melee.id, () => meleeRequests.resumeTeams(melee.id))
  const plan = melee.teamPlan
  const hasPreviousTeams = melee.teams.length > 0
  const unmarked = unmarkedPeople(melee)

  // One question at a time: first who is still unmarked, then whether those who play fit in teams
  // (the plan already counts only those who paid).
  function askFirst() {
    if (unmarked.length > 0) setAskingUnmarked(true)
    else checkFit(false)
  }

  function checkFit(didNotPay: boolean) {
    setAskingUnmarked(false)
    setUnmarkedDidNotPay(didNotPay)
    if (plan.fits) startDraw(false, didNotPay)
    else setDoesNotFit(true)
  }

  function startDraw(differentTeam: boolean, didNotPay: boolean) {
    setDoesNotFit(false)
    draw.run({ differentTeam, unmarkedDidNotPay: didNotPay })
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
        <p className="text-lg font-semibold text-red-800">
          {melee.payments ? t('steps.draw.notEnoughPaid') : t('steps.draw.notEnough')}
        </p>
      ) : (
        <Button
          variant={hasPreviousTeams ? 'secondary' : 'accent'}
          className={hasPreviousTeams ? '' : 'min-h-20 text-2xl'}
          busy={draw.isPending}
          onClick={askFirst}
        >
          {hasPreviousTeams ? t('steps.draw.redraw') : t('steps.draw.button')}
        </Button>
      )}
      <Button variant="ghost" onClick={() => setEditingSettings(true)}>
        {t('settings.button', { summary: settingsSummary(t, melee.settings) })}
      </Button>
      <ErrorMessage error={draw.error ?? resume.error} />
      {draw.dialog}
      {editingSettings && <SettingsDialog melee={melee} onClose={() => setEditingSettings(false)} />}

      <ConfirmDialog
        open={askingUnmarked}
        title={t('steps.draw.unmarked.title')}
        confirmLabel={t('steps.draw.unmarked.confirm')}
        cancelLabel={t('steps.draw.unmarked.cancel')}
        onCancel={() => setAskingUnmarked(false)}
        onConfirm={() => checkFit(true)}
      >
        <p>{t('steps.draw.unmarked.text', { count: unmarked.length, names: unmarked.map((person) => person.name).join(', ') })}</p>
      </ConfirmDialog>

      <Dialog open={doesNotFit} onClose={() => setDoesNotFit(false)} title={t('steps.draw.doesNotFitTitle')}>
        <div className="flex flex-col gap-3">
          <p className="text-lg">
            {t(melee.payments ? 'players.plan.paid.doesNotFit' : 'players.plan.doesNotFit', {
              count: plan.players,
              size: t(`melee.teamSize.${melee.teamSize}`).toLowerCase(),
            })}
          </p>
          <Button variant="accent" onClick={() => startDraw(true, unmarkedDidNotPay)}>
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
  const blocked = melee.teamIssues.teamsWithoutPlayers.length > 0

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
        {t('settings.button', { summary: settingsSummary(t, melee.settings) })}
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
  const startPrizes = useConfirmableAction(melee.id, (_: void, confirm) => meleeRequests.startPrizes(melee.id, confirm),
    () => navigate(`/melees/${melee.id}/premios`))
  const complete = melee.international?.complete ?? false
  return (
    <StepCard>
      <p className="text-xl font-bold">{complete ? t('intl.done') : t('steps.international.inProgress')}</p>
      {complete && (
        <Button variant="accent" className="min-h-20 text-2xl" busy={startPrizes.isPending} onClick={() => startPrizes.run(undefined)}>
          {t('steps.prizes.start')}
        </Button>
      )}
      <Button variant={complete ? 'secondary' : 'accent'} className="min-h-16 text-xl" onClick={() => navigate(`/melees/${melee.id}/internacional`)}>
        {t('steps.international.open')}
      </Button>
      <ErrorMessage error={startPrizes.error} />
      {startPrizes.dialog}
    </StepCard>
  )
}

function PrizesStep({ melee }: { melee: MeleeView }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  return (
    <StepCard>
      <Button variant="accent" className="min-h-20 text-2xl" onClick={() => navigate(`/melees/${melee.id}/premios`)}>
        {t('steps.prizes.open')}
      </Button>
    </StepCard>
  )
}
