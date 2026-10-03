import { useId } from 'react'
import { useTranslation } from 'react-i18next'

interface NumberStepperProps {
  label: string
  value: number
  onChange: (value: number) => void
  min: number
  max: number
  error?: string
}

/** − value + with big buttons: no tiny spinners or fine gestures. */
export function NumberStepper({ label, value, onChange, min, max, error }: NumberStepperProps) {
  const id = useId()
  const { t } = useTranslation()
  const clamp = (next: number) => Math.min(max, Math.max(min, next))
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-lg font-semibold">
        {label}
      </label>
      <div className="flex items-stretch gap-2">
        <button
          type="button"
          onClick={() => onChange(clamp(value - 1))}
          disabled={value <= min}
          aria-label={t('common.decrease', { label })}
          className="size-14 shrink-0 rounded-xl border-2 border-steel-400 bg-white text-3xl font-bold disabled:opacity-40"
        >
          −
        </button>
        <input
          id={id}
          type="number"
          inputMode="numeric"
          min={min}
          max={max}
          value={Number.isNaN(value) ? '' : value}
          onChange={(event) => onChange(clamp(Number(event.target.value)))}
          className="h-14 w-20 min-w-0 rounded-xl border-2 border-steel-400 bg-white text-center text-2xl font-bold"
        />
        <button
          type="button"
          onClick={() => onChange(clamp(value + 1))}
          disabled={value >= max}
          aria-label={t('common.increase', { label })}
          className="size-14 shrink-0 rounded-xl border-2 border-steel-400 bg-white text-3xl font-bold disabled:opacity-40"
        >
          +
        </button>
      </div>
      {error && <p className="text-base font-semibold text-red-800">{error}</p>}
    </div>
  )
}
