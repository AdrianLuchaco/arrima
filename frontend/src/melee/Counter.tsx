import { useTranslation } from 'react-i18next'
import type { MeleeView } from './types'

/**
 * The figures everyone keeps asking about. They go in the bar fixed at the bottom of the screen
 * during the matches (see BottomBar), so they are visible from any tab and any scroll position.
 */
export function CounterFigures({ melee }: { melee: MeleeView }) {
  const { t } = useTranslation()
  if (melee.counter.length === 0) return null
  return (
    <div aria-label={t('counter.label')} role="group" className="grid grid-cols-2 divide-x-2 divide-steel-600 px-2 py-2">
      {melee.counter.map((target) => (
        <div key={target.wins} className="px-3">
          <p className="text-base font-semibold">{t('counter.wins', { count: target.wins })}</p>
          <p className="flex items-baseline gap-2">
            <span className="text-4xl font-extrabold leading-none">{target.reached}</span>
            <span className="text-base">{t('counter.canReach', { count: target.canReach })}</span>
          </p>
        </div>
      ))}
    </div>
  )
}
