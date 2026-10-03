import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { formatMeleeDate } from '../lib/dates'
import { meleeRequests, useDeleteMelee, useMeleeAction } from './meleeApi'
import { STATUS_ORDER, type MeleeView } from './types'
import { Button } from '../ui/Button'
import { ConfirmDialog } from '../ui/ConfirmDialog'

/** Date, format, the six phases (current one highlighted) and the less frequent actions. */
export function MeleeHeader({ melee }: { melee: MeleeView }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const goBack = useMeleeAction(melee.id, () => meleeRequests.goBack(melee.id))
  const remove = useDeleteMelee()
  const [confirming, setConfirming] = useState<'back' | 'delete' | null>(null)
  const current = STATUS_ORDER.indexOf(melee.status)
  const canGoBack = melee.status !== 'REGISTRATION' && melee.status !== 'CLOSED'

  return (
    <header className="mb-4 flex flex-col gap-3">
      <div>
        <h1 className="text-3xl font-extrabold first-letter:uppercase">{formatMeleeDate(melee.playedOn)}</h1>
        <p className="text-lg text-steel-600">
          {t('melee.classic')} · {t(`melee.teamSize.${melee.teamSize}`)} ·{' '}
          {t('melee.summary', melee.settings)}
        </p>
      </div>

      <ol className="flex gap-1" aria-label={t('melee.phases')}>
        {STATUS_ORDER.map((status, index) => (
          <li
            key={status}
            aria-current={index === current ? 'step' : undefined}
            className={`h-3 flex-1 rounded-full ${index < current ? 'bg-steel-600' : index === current ? 'bg-jack-500' : 'bg-gravel-300'}`}
          >
            <span className="sr-only">{t(`melee.status.${status}`)}</span>
          </li>
        ))}
      </ol>
      <p className="text-lg font-bold">
        {t('melee.phaseOf', { number: current + 1, total: STATUS_ORDER.length, name: t(`melee.status.${melee.status}`) })}
      </p>

      <div className="flex flex-wrap gap-2">
        {canGoBack && (
          <Button variant="secondary" onClick={() => setConfirming('back')}>
            {t('melee.back.button', { phase: t(`melee.status.${STATUS_ORDER[current - 1]}`) })}
          </Button>
        )}
        <Button variant="ghost" onClick={() => setConfirming('delete')}>
          {t('melee.delete.button')}
        </Button>
      </div>

      <ConfirmDialog
        open={confirming === 'back'}
        title={t('melee.back.title')}
        confirmLabel={t('melee.back.confirm')}
        busy={goBack.isPending}
        error={goBack.error}
        onCancel={() => setConfirming(null)}
        onConfirm={() => goBack.mutate(undefined, { onSuccess: () => setConfirming(null) })}
      >
        <p>{t('melee.back.explanation', { phase: t(`melee.status.${STATUS_ORDER[Math.max(0, current - 1)]}`) })}</p>
      </ConfirmDialog>

      <ConfirmDialog
        open={confirming === 'delete'}
        title={t('melee.delete.title')}
        confirmLabel={t('melee.delete.confirm')}
        danger
        busy={remove.isPending}
        error={remove.error}
        onCancel={() => setConfirming(null)}
        onConfirm={() => remove.mutate(melee.id, { onSuccess: () => navigate('/') })}
      >
        <p>{t('melee.delete.explanation')}</p>
      </ConfirmDialog>
    </header>
  )
}
