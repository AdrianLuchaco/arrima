import { useTranslation } from 'react-i18next'
import { IntlBoard } from '../international/IntlBoard'
import { positionLabel } from '../international/labels'
import { teamNumber, teamPlayers } from '../melee/names'
import type { MeleeView } from '../melee/types'

/** Spectators see whose turn it is and the scoreboard, updated live. */
export function PublicInternational({ melee }: { melee: MeleeView }) {
  const { t } = useTranslation()
  const turn = melee.international?.turn
  const teamSize = turn ? (melee.teams.find((team) => team.id === turn.teamId)?.memberIds.length ?? 2) : 2
  return (
    <div className="flex flex-col gap-4">
      {turn && melee.status === 'INTERNATIONAL' && (
        <section className="rounded-2xl bg-steel-800 p-4 text-gravel-50">
          <p className="text-lg">{t('intl.nowPlaying')}</p>
          <p className="text-3xl font-extrabold">{t('teams.teamNumber', { number: teamNumber(melee, turn.teamId) })}</p>
          <p className="text-lg">{teamPlayers(melee, turn.teamId)}</p>
          <p className="mt-2 text-2xl font-extrabold text-jack-500 uppercase">
            {turn.kind === 'POINTING' ? t('intl.points_action') : t('intl.shoots_action')} · {positionLabel(t, teamSize, turn.position)} · {t('intl.ballOf', { ball: turn.ballNumber })}
          </p>
        </section>
      )}
      <IntlBoard melee={melee} />
    </div>
  )
}
