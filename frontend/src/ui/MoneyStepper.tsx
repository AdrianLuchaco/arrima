import { useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { eurosForInput, parseEuros } from '../lib/money'

interface MoneyStepperProps {
  label: string
  /** In cents. */
  value: number
  onChange: (cents: number) => void
  max: number
  help?: string
  disabled?: boolean
}

const STEP_CENTS = 50

/** − 5,00 € + in 50-cent steps, like the other settings; the amount can also be typed. */
export function MoneyStepper({ label, value, onChange, max, help, disabled = false }: MoneyStepperProps) {
  const id = useId()
  const { t } = useTranslation()
  const [text, setText] = useState(eurosForInput(value))
  const [shown, setShown] = useState(value)
  const clamp = (cents: number) => Math.min(max, Math.max(0, cents))

  // Follows changes from outside (the steps, or a reset) without fighting the typing.
  if (shown !== value) {
    setShown(value)
    setText(eurosForInput(value))
  }

  function step(delta: number) {
    onChange(clamp(value + delta))
  }

  function commit() {
    const cents = parseEuros(text)
    if (cents === null) setText(eurosForInput(value))
    else onChange(clamp(cents))
  }

  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-lg font-semibold">
        {label}
      </label>
      <div className="flex items-stretch gap-2">
        <button
          type="button"
          onClick={() => step(-STEP_CENTS)}
          disabled={disabled || value <= 0}
          aria-label={t('common.decrease', { label })}
          className="size-14 shrink-0 rounded-xl border-2 border-steel-400 bg-white text-3xl font-bold disabled:opacity-40"
        >
          −
        </button>
        <div className="relative">
          <input
            id={id}
            type="text"
            inputMode="decimal"
            disabled={disabled}
            value={text}
            onChange={(event) => setText(event.target.value)}
            onBlur={commit}
            aria-describedby={help ? `${id}-help` : undefined}
            className="h-14 w-28 min-w-0 rounded-xl border-2 border-steel-400 bg-white pr-8 text-center text-2xl font-bold disabled:opacity-60"
          />
          <span aria-hidden="true" className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-xl font-bold">
            €
          </span>
        </div>
        <button
          type="button"
          onClick={() => step(STEP_CENTS)}
          disabled={disabled || value >= max}
          aria-label={t('common.increase', { label })}
          className="size-14 shrink-0 rounded-xl border-2 border-steel-400 bg-white text-3xl font-bold disabled:opacity-40"
        >
          +
        </button>
      </div>
      {help && (
        <p id={`${id}-help`} className="text-base text-steel-600">
          {help}
        </p>
      )}
    </div>
  )
}
