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
 * It has to be unmistakable that something opened on top: the page behind goes dark, the dialog is
 * white with a strong shadow, it slides in, and it closes with a button that says «Cerrar».
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
      className={`arrima-dialog m-0 mt-auto max-h-[92dvh] w-full max-w-none flex-col rounded-t-3xl bg-white p-0 text-steel-900 shadow-[0_-12px_40px_rgba(28,34,38,0.45)] backdrop:bg-steel-900/80 backdrop:backdrop-blur-[2px] open:flex sm:m-auto sm:rounded-3xl sm:shadow-[0_24px_60px_rgba(28,34,38,0.55)] ${wide ? 'sm:max-w-3xl' : 'sm:max-w-lg'}`}
    >
      {/* The handle of a sheet that came up from the bottom, as in phone apps. */}
      <div aria-hidden="true" className="mx-auto mt-3 h-1.5 w-14 shrink-0 rounded-full bg-steel-400 sm:hidden" />
      <div className="flex shrink-0 items-center justify-between gap-3 border-b-2 border-gravel-300 px-5 pt-3 pb-4 sm:pt-4">
        <h2 id={titleId} className="text-2xl font-bold">
          {title}
        </h2>
        <button
          type="button"
          onClick={onClose}
          className="flex min-h-12 shrink-0 items-center gap-1.5 rounded-xl border-2 border-steel-400 bg-white px-3 text-lg font-bold hover:bg-gravel-100 focus-visible:outline-4 focus-visible:outline-offset-2 focus-visible:outline-jack-500"
        >
          <span aria-hidden="true" className="text-xl leading-none">✕</span>
          {t('common.close')}
        </button>
      </div>
      {/* Only the content scrolls: the title, the close button and the footer stay in view. The
          content starts at its own height and only shrinks (and scrolls) when the dialog reaches its
          maximum height: a zero flex-basis here let some browsers squash it to nothing. */}
      <div className="min-h-0 flex-initial overflow-y-auto px-5 py-5">{open && children}</div>
      {footer && <div className="shrink-0 border-t-2 border-gravel-300 bg-gravel-50 px-5 py-4 sm:rounded-b-3xl">{open && footer}</div>}
    </dialog>
  )
}
