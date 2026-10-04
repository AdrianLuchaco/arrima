import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { ScoringTable } from '../club/clubApi'
import type { ThrowKind, ThrowOutcome } from '../melee/types'
import { pointsFor } from './intl'

interface FieldProps {
  kind: ThrowKind
  scoring: ScoringTable
  onPick: (outcome: ThrowOutcome) => void
}

/** The drawing of the field for the current ball, plus big buttons for the same choices. */
export function Field({ kind, scoring, onPick }: FieldProps) {
  return kind === 'POINTING' ? <PointingField scoring={scoring} onPick={onPick} /> : <ShootingField scoring={scoring} onPick={onPick} />
}

/**
 * Pointing: two concentric circles with the jack in the centre. Tap where the ball stopped, or
 * "Fuera". The zones are drawn bigger than real size so they are easy to hit with a finger.
 */
function PointingField({ scoring, onPick }: Omit<FieldProps, 'kind'>) {
  const { t } = useTranslation()
  return (
    <div className="flex flex-col items-center gap-3">
      <svg viewBox="0 0 300 300" className="w-full max-w-64 touch-manipulation" role="group" aria-label={t('intl.field.pointing')}>
        <circle cx="150" cy="150" r="145" fill="#ede5d6" stroke="#6f624f" strokeWidth="4" onClick={() => onPick('BIG_CIRCLE')} className="cursor-pointer" />
        <circle cx="150" cy="150" r="95" fill="#f7f3ec" stroke="#6f624f" strokeWidth="4" onClick={() => onPick('SMALL_CIRCLE')} className="cursor-pointer" />
        <circle cx="150" cy="150" r="45" fill="#fde2cf" stroke="#b23a0a" strokeWidth="3" strokeDasharray="8 6" onClick={() => onPick('NEAR_JACK')} className="cursor-pointer" />
        <circle cx="150" cy="150" r="18" fill="#e8590c" onClick={() => onPick('ON_JACK')} className="cursor-pointer" />
      </svg>
      <OutcomeButtons outcomes={['ON_JACK', 'NEAR_JACK', 'SMALL_CIRCLE', 'BIG_CIRCLE', 'OUT']} scoring={scoring} onPick={onPick} />
    </div>
  )
}

/**
 * Shooting: the small circle drawn with a thick border (the iron ring) and the target ball in the
 * centre. Tap the ball to choose "De 1", "De 2" or "Carro"; "Fallo" is always there.
 */
function ShootingField({ scoring, onPick }: Omit<FieldProps, 'kind'>) {
  const { t } = useTranslation()
  const [ballTapped, setBallTapped] = useState(false)
  return (
    <div className="flex flex-col items-center gap-3">
      <svg viewBox="0 0 300 300" className="w-full max-w-52 touch-manipulation" role="group" aria-label={t('intl.field.shooting')}>
        <defs>
          <radialGradient id="target-ball" cx="35%" cy="30%" r="75%">
            <stop offset="0" stopColor="#f4f6f7" />
            <stop offset="0.45" stopColor="#9aa5ad" />
            <stop offset="1" stopColor="#2c3439" />
          </radialGradient>
        </defs>
        <circle cx="150" cy="150" r="125" fill="#f7f3ec" stroke="#56636d" strokeWidth="14" />
        <circle cx="150" cy="150" r="48" fill="url(#target-ball)" onClick={() => setBallTapped(true)} className="cursor-pointer" />
        {!ballTapped && (
          <text x="150" y="235" textAnchor="middle" fontSize="20" fill="#56636d">{t('intl.field.tapBall')}</text>
        )}
      </svg>
      {ballTapped ? (
        <OutcomeButtons outcomes={['CARREAU', 'HIT_OUT', 'HIT']} scoring={scoring} onPick={onPick} />
      ) : (
        <button type="button" onClick={() => setBallTapped(true)} className="min-h-14 w-full rounded-2xl border-2 border-steel-600 bg-white text-xl font-bold">
          {t('intl.field.hitTheBall')}
        </button>
      )}
      <OutcomeButtons outcomes={['MISS']} scoring={scoring} onPick={onPick} />
    </div>
  )
}

export function OutcomeButtons({ outcomes, scoring, onPick }: { outcomes: ThrowOutcome[]; scoring: ScoringTable; onPick: (outcome: ThrowOutcome) => void }) {
  const { t } = useTranslation()
  return (
    <div className="grid w-full gap-2">
      {outcomes.map((outcome) => (
        <button
          key={outcome}
          type="button"
          onClick={() => onPick(outcome)}
          className="flex min-h-16 items-center justify-between rounded-2xl border-2 border-steel-400 bg-white px-4 text-xl font-bold"
        >
          <span>{t(`intl.outcome.${outcome}`)}</span>
          <span className="rounded-xl bg-steel-800 px-3 py-1 text-white">{t('intl.points', { count: pointsFor(scoring, outcome) })}</span>
        </button>
      ))}
    </div>
  )
}
