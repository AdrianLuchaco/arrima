import { useId, useState } from 'react'
import { useTranslation } from 'react-i18next'

interface PasswordFieldProps {
  label: string
  value: string
  onChange: (value: string) => void
  autoComplete: 'current-password' | 'new-password'
  help?: string
  error?: string
}

/** Password with a large "Mostrar" button: typing blind on a phone in the sun is error-prone. */
export function PasswordField({ label, value, onChange, autoComplete, help, error }: PasswordFieldProps) {
  const id = useId()
  const { t } = useTranslation()
  const [visible, setVisible] = useState(false)
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-lg font-semibold">
        {label}
      </label>
      <div className="flex gap-2">
        <input
          id={id}
          type={visible ? 'text' : 'password'}
          autoComplete={autoComplete}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          aria-invalid={error ? true : undefined}
          aria-describedby={help || error ? `${id}-note` : undefined}
          className={`h-14 min-w-0 flex-1 rounded-xl border-2 bg-white px-4 text-lg focus:outline-4 focus:outline-offset-1 focus:outline-jack-500 ${error ? 'border-red-700' : 'border-steel-400'}`}
        />
        <button
          type="button"
          onClick={() => setVisible((current) => !current)}
          aria-pressed={visible}
          className="h-14 shrink-0 rounded-xl border-2 border-steel-400 bg-white px-3 text-base font-semibold"
        >
          {visible ? t('auth.hidePassword') : t('auth.showPassword')}
        </button>
      </div>
      {(help || error) && (
        <p id={`${id}-note`} className={error ? 'text-base font-semibold text-red-800' : 'text-base text-steel-600'}>
          {error ?? help}
        </p>
      )}
    </div>
  )
}
