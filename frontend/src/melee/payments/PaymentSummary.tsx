import { useTranslation } from 'react-i18next'
import { formatEuros } from '../../lib/money'
import type { Payments } from '../types'

/**
 * "34 de 35 han pagado · 170 €", and how many are still unmarked. Fixed at the bottom while payments
 * are being collected (like the win counter during the matches); a plain card in the history.
 */
export function PaymentSummary({ payments, fixed }: { payments: Payments; fixed: boolean }) {
  const { t } = useTranslation()
  const content = (
    <>
      <p className="text-xl font-bold">
        {t('players.payment.summary', {
          paid: payments.paid,
          expected: payments.expected,
          amount: formatEuros(payments.collectedCents),
        })}
      </p>
      <p className="text-lg">
        {payments.unmarked > 0 ? t('players.payment.unmarked', { count: payments.unmarked }) : t('players.payment.allMarked')}
        {payments.unpaid > 0 && ` · ${t('players.payment.unpaid', { count: payments.unpaid })}`}
      </p>
    </>
  )
  if (!fixed) {
    return <section className="rounded-2xl border-2 border-gravel-300 bg-white px-4 py-3">{content}</section>
  }
  return (
    <section
      aria-live="polite"
      className="fixed inset-x-0 bottom-0 z-20 border-t-4 border-green-700 bg-steel-800 text-gravel-50 shadow-[0_-4px_12px_rgba(0,0,0,0.25)]"
    >
      <div className="mx-auto max-w-4xl px-4 py-2">{content}</div>
    </section>
  )
}
