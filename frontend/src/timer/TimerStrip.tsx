import { useTranslation } from 'react-i18next'
import { formatCountdown } from './countdown'
import type { CountdownState } from './countdown'

/** «Partida 1 · 44:59» in big figures, at the bottom of every screen of the melee. */
export function TimerStrip({ countdown }: { countdown: CountdownState }) {
  const { t } = useTranslation()
  const { round, timer, remaining } = countdown
  const label =
    timer.state === 'PAUSED' ? t('timer.strip.paused', { round })
      : timer.state === 'ENDED' ? t('timer.strip.timeUp', { round })
        : t('timer.strip.running', { round })
  return (
    <div role="timer" aria-live="off" className="flex items-center justify-between gap-3 border-b-2 border-steel-600 px-4 py-2">
      <p className="text-xl font-bold">{label}</p>
      {timer.state !== 'ENDED' && (
        <p className={`text-5xl leading-none font-extrabold tabular-nums ${timer.state === 'PAUSED' ? 'opacity-60' : ''}`}>
          {formatCountdown(remaining)}
        </p>
      )}
    </div>
  )
}

