import { useId, type InputHTMLAttributes } from 'react'

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  help?: string
  error?: string
}

export function TextField({ label, help, error, className = '', ...props }: TextFieldProps) {
  const id = useId()
  const describedBy = [help && `${id}-help`, error && `${id}-error`].filter(Boolean).join(' ') || undefined
  return (
    <div className={`flex flex-col gap-1 ${className}`}>
      <label htmlFor={id} className="text-lg font-semibold">
        {label}
      </label>
      <input
        id={id}
        {...props}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={`h-14 rounded-xl border-2 bg-white px-4 text-lg focus:outline-4 focus:outline-offset-1 focus:outline-jack-500 ${error ? 'border-red-700' : 'border-steel-400'}`}
      />
      {help && (
        <p id={`${id}-help`} className="text-base text-steel-600">
          {help}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} className="text-base font-semibold text-red-800">
          {error}
        </p>
      )}
    </div>
  )
}
