import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '../../ui/Button'
import { teamPlayers } from '../names'
import type { MeleeView } from '../types'

const SPIN_MS = 1800

/**
 * A short "bingo drum" moment after the draw, like the real one at the club: the drum spins, then
 * the teams appear one after another. "Saltar" ends it at any time; with reduced motion it is skipped.
 */
export function BomboOverlay({ melee, onDone }: { melee: MeleeView; onDone: () => void }) {
  const { t } = useTranslation()
  const [revealed, setRevealed] = useState(false)
  // The latest onDone without restarting the timer when the parent re-renders (live updates).
  const done = useRef(onDone)
  useEffect(() => {
    done.current = onDone
  }, [onDone])

  useEffect(() => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      done.current()
      return
    }
    const timer = setTimeout(() => setRevealed(true), SPIN_MS)
    return () => clearTimeout(timer)
  }, [])

  return (
    <div role="dialog" aria-modal="true" aria-label={t('bombo.label')} className="fixed inset-0 z-50 flex flex-col bg-steel-900/95 text-gravel-50">
      <div className="flex justify-end p-4">
        <Button variant="secondary" onClick={onDone}>
          {revealed ? t('bombo.close') : t('bombo.skip')}
        </Button>
      </div>
      {!revealed ? (
        <div className="flex flex-1 flex-col items-center justify-center gap-6">
          <svg viewBox="0 0 200 200" className="arrima-drum size-56" aria-hidden="true">
            <circle cx="100" cy="100" r="90" fill="none" stroke="#cbbda3" strokeWidth="8" />
            {Array.from({ length: 8 }, (_, index) => {
              const angle = (index / 8) * 2 * Math.PI
              return (
                <circle key={index} cx={100 + 55 * Math.cos(angle)} cy={100 + 55 * Math.sin(angle)} r="16"
                  fill={index % 3 === 0 ? '#e8590c' : '#9aa5ad'} />
              )
            })}
          </svg>
          <p className="text-3xl font-extrabold">{t('bombo.drawing')}</p>
        </div>
      ) : (
        <ul className="mx-auto grid w-full max-w-3xl flex-1 content-start gap-2 overflow-y-auto px-4 pb-6 sm:grid-cols-2">
          {melee.teams.map((team, index) => (
            <li key={team.id} className="arrima-reveal rounded-2xl bg-gravel-50 px-4 py-3 text-steel-900" style={{ animationDelay: `${Math.min(index * 80, 2000)}ms` }}>
              <p className="text-xl font-extrabold">{t('teams.teamNumber', { number: team.number })}</p>
              <p className="text-lg">{teamPlayers(melee, team.id)}</p>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
