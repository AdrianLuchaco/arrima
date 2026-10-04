import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { MeleeView, Participant } from '../types'
import { Button } from '../../ui/Button'
import { PaymentDialog } from '../payments/PaymentDialog'
import { canRecordPayments } from '../payments/payments'
import { PaymentSummary } from '../payments/PaymentSummary'
import { ParticipantDialog } from './ParticipantDialog'
import { PasteListDialog } from './PasteListDialog'
import { TeamPlanBanner } from './TeamPlanBanner'

export function PlayersTab({ melee, readOnly = false }: { melee: MeleeView; readOnly?: boolean }) {
  const { t } = useTranslation()
  const [pasting, setPasting] = useState(false)
  const [editing, setEditing] = useState<Participant | 'new' | null>(null)
  const [askingPayment, setAskingPayment] = useState<Participant | null>(null)
  const canSignUp = !readOnly && ['REGISTRATION', 'TEAMS', 'MATCHES'].includes(melee.status)
  const canEdit = !readOnly && melee.status !== 'CLOSED'
  const recordsPayments = !readOnly && canRecordPayments(melee)

  // At the table, tapping a name asks whether they paid; the usual edit dialog is one tap further.
  function open(participant: Participant) {
    if (recordsPayments && participant.status === 'ACTIVE') setAskingPayment(participant)
    else setEditing(participant)
  }

  return (
    <div className="flex flex-col gap-4">
      {melee.status === 'REGISTRATION' && <TeamPlanBanner melee={melee} />}
      {/* While collecting, the summary is fixed at the bottom of the screen (see MeleePage). */}
      {melee.payments && !recordsPayments && <PaymentSummary payments={melee.payments} fixed={false} />}

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
            const content = <ParticipantRow participant={participant} />
            return (
              <li key={participant.id}>
                {canEdit ? (
                  <button type="button" onClick={() => open(participant)} className="flex min-h-14 w-full items-center gap-3 px-3 py-2 text-left hover:bg-gravel-50">
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
          {askingPayment && (
            <PaymentDialog
              melee={melee}
              participant={askingPayment}
              onClose={() => setAskingPayment(null)}
              onEdit={() => {
                setEditing(askingPayment)
                setAskingPayment(null)
              }}
            />
          )}
        </>
      )}
    </div>
  )
}

/**
 * Number, name and a label: never colour alone, the list is read in full sun.
 * Did not come: grey and crossed out. Paid: green tick. Did not pay: red cross. Spectators never see
 * payments; after the draw they see "No juega" for whoever is not playing.
 */
function ParticipantRow({ participant }: { participant: Participant }) {
  const { t } = useTranslation()
  const withdrawn = participant.status === 'WITHDRAWN'
  const payment = withdrawn ? null : participant.paymentStatus
  return (
    <>
      <span className="w-12 shrink-0 text-right text-xl font-bold text-steel-600">{participant.listNumber ?? '–'}</span>
      <span className={`min-w-0 flex-1 text-xl ${withdrawn ? 'text-steel-600 line-through' : payment === 'UNPAID' ? 'text-red-800' : ''}`}>
        {participant.name}
      </span>
      {withdrawn && <Label className="bg-steel-200 text-steel-900">{t('players.withdrawn')}</Label>}
      {payment === 'PAID' && <Label className="bg-green-800 text-white">✓ {t('players.payment.paid')}</Label>}
      {payment === 'UNPAID' && <Label className="bg-red-700 text-white">✗ {t('players.payment.unpaid')}</Label>}
      {payment === null && participant.notPlaying && <Label className="bg-steel-200 text-steel-900">{t('players.notPlaying')}</Label>}
    </>
  )
}

function Label({ className, children }: { className: string; children: React.ReactNode }) {
  return <span className={`shrink-0 rounded-lg px-2 py-1 text-sm font-bold ${className}`}>{children}</span>
}
