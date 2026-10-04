import { useTranslation } from 'react-i18next'
import { formatEuros } from '../../lib/money'
import { enqueue } from '../../offline/outbox'
import { Button } from '../../ui/Button'
import { Dialog } from '../../ui/Dialog'
import type { MeleeView, Participant, PaymentStatus } from '../types'

interface PaymentDialogProps {
  melee: MeleeView
  participant: Participant
  onClose: () => void
  /** The usual edit dialog (name, number, withdrawal). */
  onEdit: () => void
}

/**
 * "¿Ha pagado Manuel?" at the table: one tap on the name, one on Sí or No. The answer goes through
 * the offline queue, like a result: it shows at once and is sent when there is signal.
 */
export function PaymentDialog({ melee, participant, onClose, onEdit }: PaymentDialogProps) {
  const { t } = useTranslation()
  const current = participant.paymentStatus ?? 'UNMARKED'

  function record(paymentStatus: PaymentStatus) {
    enqueue({
      key: `payment:${participant.id}`,
      meleeId: melee.id,
      path: `/api/melees/${melee.id}/participants/${participant.id}/payment`,
      body: { paymentStatus },
      overlay: { kind: 'payment', participantId: participant.id, paymentStatus },
    })
    onClose()
  }

  return (
    <Dialog open onClose={onClose} title={t('players.payment.title', { name: participant.name })}>
      <div className="flex flex-col gap-4">
        <p className="text-lg text-steel-600">
          {t('players.payment.fee', { amount: formatEuros(melee.settings.entryFeeCents) })} · {t(`players.payment.current.${current}`)}
        </p>
        {melee.status === 'TEAMS' && (
          <p className="rounded-xl border-2 border-amber-600 bg-amber-50 px-3 py-2 text-lg text-amber-950">{t('players.payment.afterDraw')}</p>
        )}
        <div className="grid grid-cols-2 gap-3">
          <button
            type="button"
            onClick={() => record('PAID')}
            className="min-h-20 rounded-2xl bg-green-800 px-3 text-xl font-bold text-white hover:bg-green-900"
          >
            ✓ {t('players.payment.yes')}
          </button>
          <button
            type="button"
            onClick={() => record('UNPAID')}
            className="min-h-20 rounded-2xl bg-red-700 px-3 text-xl font-bold text-white hover:bg-red-800"
          >
            ✗ {t('players.payment.no')}
          </button>
        </div>
        {current !== 'UNMARKED' && (
          <Button variant="secondary" onClick={() => record('UNMARKED')}>
            {t('players.payment.clear')}
          </Button>
        )}
        <Button variant="ghost" onClick={onEdit}>
          {t('players.payment.edit')}
        </Button>
      </div>
    </Dialog>
  )
}
