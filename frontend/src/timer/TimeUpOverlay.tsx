import { useTranslation } from 'react-i18next'

/** «¡Tiempo!» over the whole screen while the alarm sounds, until someone taps «Entendido». */
export function TimeUpOverlay({ round, onClose }: { round: number; onClose: () => void }) {
  const { t } = useTranslation()
  return (
    <div
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="time-up-title"
      className="fixed inset-0 z-50 flex flex-col items-center justify-center gap-6 bg-jack-700 px-6 text-center text-white"
    >
      <p id="time-up-title" className="text-6xl font-extrabold">
        {t('timer.timeUp.title')}
      </p>
      <p className="text-3xl font-bold">{t('timer.timeUp.round', { round })}</p>
      <p className="max-w-md text-2xl">{t('timer.timeUp.tie')}</p>
      <button
        type="button"
        onClick={onClose}
        className="mt-4 min-h-16 w-full max-w-sm rounded-2xl bg-white px-6 text-2xl font-bold text-jack-800"
      >
        {t('timer.timeUp.ok')}
      </button>
    </div>
  )
}
