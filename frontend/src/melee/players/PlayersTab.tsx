import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { MeleeView, Participant } from '../types'
import { Button } from '../../ui/Button'
import { ParticipantDialog } from './ParticipantDialog'
import { PasteListDialog } from './PasteListDialog'
import { TeamPlanBanner } from './TeamPlanBanner'

export function PlayersTab({ melee, readOnly = false }: { melee: MeleeView; readOnly?: boolean }) {
  const { t } = useTranslation()
  const [pasting, setPasting] = useState(false)
  const [editing, setEditing] = useState<Participant | 'new' | null>(null)
  const canSignUp = !readOnly && ['REGISTRATION', 'TEAMS', 'MATCHES'].includes(melee.status)
  const canEdit = !readOnly && melee.status !== 'CLOSED'

  return (
    <div className="flex flex-col gap-4">
      {melee.status === 'REGISTRATION' && <TeamPlanBanner melee={melee} />}

      {canSignUp && (
        <div className="grid gap-3 sm:grid-cols-2">
          <Button onClick={() => setPasting(true)}>{t('players.paste.button')}</Button>
          <Button variant="secondary" onClick={() => setEditing('new')}>
            {t('players.add.button')}
          </Button>
        </div>
      )}

      {melee.participants.length === 0 ? (
        <p className="text-lg text-steel-600">{t('players.empty')}</p>
      ) : (
        <ul className="flex flex-col divide-y-2 divide-gravel-100 rounded-2xl border-2 border-gravel-300 bg-white">
          {melee.participants.map((participant) => {
            const withdrawn = participant.status === 'WITHDRAWN'
            const content = (
              <>
                <span className="w-12 shrink-0 text-right text-xl font-bold text-steel-600">{participant.listNumber ?? '–'}</span>
                <span className={`min-w-0 flex-1 text-xl ${withdrawn ? 'text-red-800 line-through' : ''}`}>{participant.name}</span>
                {withdrawn && <span className="rounded-lg bg-red-700 px-2 py-1 text-sm font-bold text-white">{t('players.withdrawn')}</span>}
              </>
            )
            return (
              <li key={participant.id}>
                {canEdit ? (
                  <button type="button" onClick={() => setEditing(participant)} className="flex min-h-14 w-full items-center gap-3 px-3 py-2 text-left hover:bg-gravel-50">
                    {content}
                  </button>
                ) : (
                  <div className="flex min-h-14 items-center gap-3 px-3 py-2">{content}</div>
                )}
              </li>
            )
          })}
        </ul>
      )}

      {!readOnly && (
        <>
          <PasteListDialog melee={melee} open={pasting} onClose={() => setPasting(false)} />
          <ParticipantDialog
            melee={melee}
            participant={editing === 'new' ? null : editing}
            open={editing !== null}
            onClose={() => setEditing(null)}
          />
        </>
      )}
    </div>
  )
}
