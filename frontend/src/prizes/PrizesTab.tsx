import { useTranslation } from 'react-i18next'
import { ordinalBeforeNoun } from '../lib/ordinal'
import { teamNumber, teamPlayers } from '../melee/names'
import type { MeleeView } from '../melee/types'
import { PrizePhotos } from './PrizePhotos'

/** The prizes with their photos, read-only: for spectators, and for the history of closed melees. */
export function PrizesTab({ melee }: { melee: MeleeView }) {
  const { t } = useTranslation()
  if (melee.prizes.length === 0) return <p className="text-lg text-steel-600">{t('prizes.noneYet')}</p>
  return (
    <ol className="flex flex-col gap-4">
      {melee.prizes.map((prize) => (
        <li key={prize.id} className="rounded-2xl border-2 border-gravel-300 bg-white p-4">
          <p className="text-2xl font-extrabold">{t('prizes.position', { ordinal: ordinalBeforeNoun(prize.position) })}</p>
          <p className="text-lg">
            <strong>{t('teams.teamNumber', { number: teamNumber(melee, prize.teamId) })}</strong> · {teamPlayers(melee, prize.teamId)}
            {prize.points !== null && ` · ${t('intl.points', { count: prize.points })}`}
          </p>
          {prize.photos.length > 0 && (
            <div className="mt-3">
              <PrizePhotos melee={melee} prize={prize} editable={false} />
            </div>
          )}
        </li>
      ))}
    </ol>
  )
}
