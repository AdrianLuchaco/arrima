import type { ButtonHTMLAttributes } from 'react'

type Variant = 'primary' | 'accent' | 'secondary' | 'danger' | 'ghost'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  busy?: boolean
}

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-steel-800 text-white hover:bg-steel-900',
  accent: 'bg-jack-700 text-white hover:bg-jack-800',
  secondary: 'bg-white text-steel-900 border-2 border-steel-400 hover:bg-gravel-100',
  danger: 'bg-white text-red-800 border-2 border-red-700 hover:bg-red-50',
  ghost: 'bg-transparent text-steel-800 underline underline-offset-4 hover:bg-gravel-100',
}

/** Big, high-contrast buttons: used standing, outdoors, often by older people. */
export function Button({ variant = 'primary', busy = false, className = '', disabled, children, ...props }: ButtonProps) {
  return (
    <button
      type="button"
      {...props}
      disabled={disabled || busy}
      aria-busy={busy || undefined}
      className={`inline-flex min-h-14 items-center justify-center gap-2 rounded-2xl px-5 text-lg font-bold transition-colors focus-visible:outline-4 focus-visible:outline-offset-2 focus-visible:outline-jack-500 disabled:cursor-not-allowed disabled:opacity-50 ${VARIANTS[variant]} ${className}`}
    >
      {busy && <span aria-hidden="true" className="size-5 animate-spin rounded-full border-4 border-current border-r-transparent" />}
      {children}
    </button>
  )
}
