import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ApiError } from '../api/ApiError'
import { ConfirmDialog } from '../ui/ConfirmDialog'
import { useMeleeAction } from './meleeApi'
import type { MeleeView } from './types'

interface Losses {
  results: number
  ballThrows: number
  photos: number
}

/**
 * Runs an action that may throw work away. The backend answers CONFIRMATION_REQUIRED with what
 * would be lost ("se perderán 14 resultados"); we show it and repeat the call only if confirmed.
 */
export function useConfirmableAction<V = void>(
  meleeId: number,
  request: (variables: V, confirmLosses: boolean) => Promise<MeleeView>,
  onDone?: (view: MeleeView) => void,
) {
  const { t } = useTranslation()
  const [pending, setPending] = useState<{ losses: Losses; variables: V; onSuccess?: (view: MeleeView) => void } | null>(null)
  const action = useMeleeAction(
    meleeId,
    ({ variables, confirm }: { variables: V; confirm: boolean }) => request(variables, confirm),
    onDone,
  )
  const losses = pending?.losses ?? null

  function run(variables: V, onSuccess?: (view: MeleeView) => void) {
    action.mutate(
      { variables, confirm: false },
      {
        onSuccess,
        onError: (error) => {
          if (error instanceof ApiError && error.code === 'CONFIRMATION_REQUIRED') {
            action.reset()
            setPending({ losses: error.details.losses as Losses, variables, onSuccess })
          }
        },
      },
    )
  }

  function confirm() {
    if (!pending) return
    action.mutate(
      { variables: pending.variables, confirm: true },
      {
        onSuccess: (view) => {
          setPending(null)
          pending.onSuccess?.(view)
        },
      },
    )
  }

  const dialog = (
    <ConfirmDialog
      open={losses !== null}
      title={t('losses.title')}
      confirmLabel={t('losses.confirm')}
      danger
      busy={action.isPending}
      error={action.error}
      onCancel={() => setPending(null)}
      onConfirm={confirm}
    >
      <p>{t('losses.intro')}</p>
      <ul className="list-disc pl-6">
        {losses && losses.results > 0 && <li>{t('losses.results', { count: losses.results })}</li>}
        {losses && losses.ballThrows > 0 && <li>{t('losses.ballThrows', { count: losses.ballThrows })}</li>}
        {losses && losses.photos > 0 && <li>{t('losses.photos', { count: losses.photos })}</li>}
      </ul>
    </ConfirmDialog>
  )

  return { run, dialog, isPending: action.isPending, error: losses ? null : action.error }
}
