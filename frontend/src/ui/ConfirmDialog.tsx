import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from './Button'
import { Dialog } from './Dialog'
import { ErrorMessage } from './ErrorMessage'

interface ConfirmDialogProps {
  open: boolean
  title: string
  children?: ReactNode
  confirmLabel: string
  danger?: boolean
  busy?: boolean
  error?: unknown
  onConfirm: () => void
  onCancel: () => void
}

/** "Are you sure?" with two big buttons: actions that change the melee are never one accidental tap away. */
export function ConfirmDialog({ open, title, children, confirmLabel, danger, busy, error, onConfirm, onCancel }: ConfirmDialogProps) {
  const { t } = useTranslation()
  return (
    <Dialog open={open} onClose={onCancel} title={title}>
      <div className="flex flex-col gap-5 text-lg">
        {children}
        <ErrorMessage error={error} />
        <div className="grid grid-cols-2 gap-3">
          <Button variant="secondary" onClick={onCancel}>
            {t('common.cancel')}
          </Button>
          <Button variant={danger ? 'danger' : 'primary'} busy={busy} onClick={onConfirm}>
            {confirmLabel}
          </Button>
        </div>
      </div>
    </Dialog>
  )
}
