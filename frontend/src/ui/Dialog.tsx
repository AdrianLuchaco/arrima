import { useEffect, useId, useRef, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'

interface DialogProps {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  /** Wide dialogs (previews, tables) use the whole screen on phones. */
  wide?: boolean
  /** The main action: fixed under the content, always in view however long the content is. */
  footer?: ReactNode
}

/**
 * Native <dialog>: the browser handles focus trapping, the Escape key and screen readers.
 * On phones it takes the full width at the bottom, within thumb reach.
 */
export function Dialog({ open, onClose, title, children, wide = false, footer }: DialogProps) {
  const { t } = useTranslation()
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  // The browser fires "close" also when we close it because `open` became false. Only a close by
  // the user (Escape, the × button) must call onClose: the parent may already be showing the next step.
  const closingFromProps = useRef(false)

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) {
      closingFromProps.current = true
      dialog.close()
    }
  }, [open])

  function handleClose() {
    if (closingFromProps.current) {
      closingFromProps.current = false
      return
    }
    onClose()
  }

  return (
    <dialog
      ref={ref}
      onClose={handleClose}
      aria-labelledby={titleId}
      className={`m-0 mt-auto max-h-[92dvh] w-full max-w-none flex-col rounded-t-3xl bg-gravel-50 p-0 text-steel-900 backdrop:bg-steel-900/60 open:flex sm:m-auto sm:rounded-3xl ${wide ? 'sm:max-w-3xl' : 'sm:max-w-lg'}`}
    >
      <div className="flex shrink-0 items-center justify-between gap-3 border-b-2 border-gravel-300 px-5 py-4">
        <h2 id={titleId} className="text-2xl font-bold">
          {title}
        </h2>
        <button
          type="button"
          onClick={onClose}
          className="size-12 shrink-0 rounded-xl border-2 border-steel-400 bg-white text-2xl font-bold"
          aria-label={t('common.close')}
        >
          ×
        </button>
      </div>
      {/* Only the content scrolls: the title, the close button and the footer stay in view. */}
      <div className="min-h-0 flex-1 overflow-y-auto px-5 py-5">{open && children}</div>
      {footer && <div className="shrink-0 border-t-2 border-gravel-300 px-5 py-4">{open && footer}</div>}
    </dialog>
  )
}
