import { useTranslation } from 'react-i18next'
import { dismissRejected, flush } from '../offline/outbox'
import { useOnline, useOutbox } from '../offline/useOutbox'
import { useErrorText } from './useErrorText'

/**
 * Always visible in the admin area: whether there is connection and how many taps are still waiting
 * to be sent. Nothing is lost meanwhile; this only tells the admin what is going on.
 */
export function ConnectionBadge() {
  const { t } = useTranslation()
  const online = useOnline()
  const { pending, rejected } = useOutbox()
  const errors = useErrorText()

  return (
    <div aria-live="polite">
      {(!online || pending.length > 0) && (
        <button
          type="button"
          onClick={() => void flush()}
          className={`w-full px-4 py-2 text-center text-lg font-bold ${online ? 'bg-amber-200 text-amber-950' : 'bg-red-700 text-white'}`}
        >
          {!online ? t('connection.offline') : t('connection.sending')}
          {pending.length > 0 && ` · ${t('connection.pending', { count: pending.length })}`}
        </button>
      )}
      {rejected && (
        <div role="alert" className="flex items-center justify-between gap-3 bg-red-100 px-4 py-2 text-lg text-red-900">
          <span>{t('connection.rejected', { reason: errors.message(rejected.error) })}</span>
          <button type="button" onClick={dismissRejected} className="rounded-lg border-2 border-red-800 px-3 py-1 font-bold">
            {t('common.close')}
          </button>
        </div>
      )}
    </div>
  )
}
