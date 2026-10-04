import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { meleeRequests, useMeleeAction } from '../melee/meleeApi'
import type { MeleeView } from '../melee/types'
import { isSubscribed, pushSupport, subscribe, type PushSupport } from '../push/push'
import { Button } from '../ui/Button'
import { ConfirmDialog } from '../ui/ConfirmDialog'
import { ErrorMessage } from '../ui/ErrorMessage'
import { unlockAudio } from './alarm'
import { nextRoundToStart } from './countdown'

type Step = 'start' | 'pause' | 'resume' | 'cancel'

/**
 * The admin's controls of the countdown, above the court sheet: «Empezar partida N», and while one
 * runs, pause/resume and cancel. Everything asks first: these change what every phone shows.
 */
export function TimerPanel({ melee }: { melee: MeleeView }) {
  const { t } = useTranslation()
  const [asking, setAsking] = useState<Step | null>(null)
  const [support, setSupport] = useState<PushSupport>('unsupported')
  const next = nextRoundToStart(melee)
  const live = melee.rounds.find((round) => round.timer !== null && round.timer.state !== 'ENDED') ?? null
  const action = useMeleeAction(melee.id, (step: Step) => {
    switch (step) {
      case 'start':
        return meleeRequests.startTimer(melee.id, next!, live !== null)
      case 'pause':
        return meleeRequests.pauseTimer(melee.id, live!.number)
      case 'resume':
        return meleeRequests.resumeTimer(melee.id, live!.number)
      case 'cancel':
        return meleeRequests.cancelTimer(melee.id, live!.number)
    }
  })

  useEffect(() => {
    void pushSupport().then(setSupport)
  }, [])

  if (melee.status !== 'MATCHES') return null

  function confirm(step: Step) {
    if (step === 'start') {
      // This tap is the admin's "yes": the moment the browser lets us unlock the sound and ask for
      // notifications (the admin's phone is the official alarm of the table).
      unlockAudio()
      const target = { kind: 'admin', meleeId: melee.id } as const
      if (support === 'supported' && !isSubscribed(target)) void subscribe(target).catch(() => undefined)
    }
    action.mutate(step, { onSuccess: () => setAsking(null) })
  }

  const previousPending = next !== null && next > 1
    ? melee.rounds[next - 2].matches.filter((match) => match.winnerTeamId === null).length
    : 0

  return (
    <section className="flex flex-col gap-3 rounded-2xl border-2 border-steel-600 bg-white p-4">
      {live && (
        <>
          <p className="text-xl font-bold">
            {live.timer!.state === 'PAUSED' ? t('timer.panel.paused', { round: live.number }) : t('timer.panel.running', { round: live.number })}
          </p>
          <div className="grid grid-cols-2 gap-3">
            {live.timer!.state === 'PAUSED' ? (
              <Button onClick={() => setAsking('resume')}>{t('timer.resume')}</Button>
            ) : (
              <Button variant="secondary" onClick={() => setAsking('pause')}>{t('timer.pause')}</Button>
            )}
            <Button variant="ghost" onClick={() => setAsking('cancel')}>{t('timer.cancel')}</Button>
          </div>
        </>
      )}
      {next !== null && (
        <Button variant={live ? 'secondary' : 'accent'} className={live ? '' : 'min-h-20 text-2xl'} onClick={() => setAsking('start')}>
          {t('timer.start', { round: next })}
        </Button>
      )}
      <ErrorMessage error={asking === null ? action.error : null} />

      <ConfirmDialog
        open={asking !== null}
        title={asking === 'start' ? t('timer.ask.startTitle', { round: next }) : t(`timer.ask.${asking ?? 'pause'}Title`, { round: live?.number })}
        confirmLabel={t('common.yes')}
        cancelLabel={t('common.no')}
        danger={asking === 'cancel'}
        busy={action.isPending}
        error={action.error}
        onCancel={() => setAsking(null)}
        onConfirm={() => asking && confirm(asking)}
      >
        {asking === 'start' && (
          <>
            <p>{t('timer.ask.start', { round: next, minutes: melee.settings.matchMinutes })}</p>
            {previousPending > 0 && <p className="font-semibold">{t('timer.ask.pendingResults', { count: previousPending, round: next! - 1 })}</p>}
            {live && <p className="font-semibold">{t('timer.ask.stopsRunning', { round: live.number })}</p>}
          </>
        )}
        {asking === 'pause' && <p>{t('timer.ask.pause')}</p>}
        {asking === 'resume' && <p>{t('timer.ask.resume')}</p>}
        {asking === 'cancel' && <p>{t('timer.ask.cancel')}</p>}
      </ConfirmDialog>
    </section>
  )
}
