interface LoadingScreenProps {
  title: string
  detail?: string
  /** Hides the title visually (still read by screen readers) for short, quiet loads. */
  quiet?: boolean
}

const BALL_POSITIONS = [96, 134, 166]

/**
 * Steel balls rolling up to the jack. Pure SVG + CSS: no animation library, so it loads instantly
 * on a slow connection. It is also the "waking up the server" screen.
 */
export function LoadingScreen({ title, detail, quiet = false }: LoadingScreenProps) {
  return (
    <main className="flex min-h-dvh flex-col items-center justify-center gap-6 px-6 text-center">
      <PetanqueAnimation />
      <div role="status" aria-live="polite" className={quiet ? 'sr-only' : 'max-w-sm space-y-2'}>
        <p className="text-2xl font-bold">{title}</p>
        {detail && <p className="text-lg text-steel-600">{detail}</p>}
      </div>
    </main>
  )
}

function PetanqueAnimation() {
  return (
    <svg viewBox="0 0 240 90" className="w-64 max-w-full" aria-hidden="true">
      <defs>
        <radialGradient id="arrima-steel" cx="35%" cy="30%" r="75%">
          <stop offset="0" stopColor="#f4f6f7" />
          <stop offset="0.45" stopColor="#9aa5ad" />
          <stop offset="1" stopColor="#2c3439" />
        </radialGradient>
      </defs>
      <line x1="4" y1="80" x2="236" y2="80" stroke="#cbbda3" strokeWidth="4" strokeLinecap="round" />
      <circle cx="206" cy="71" r="7" fill="#e8590c" />
      {BALL_POSITIONS.map((x, index) => (
        <g key={x} className="arrima-ball" style={{ animationDelay: `${index * 0.35}s` }}>
          <circle cx={x} cy="62" r="16" fill="url(#arrima-steel)" />
          <path
            d={`M${x - 15} 56c9 5 21 5 30 0M${x - 15} 68c9-5 21-5 30 0`}
            fill="none"
            stroke="#2c3439"
            strokeWidth="1.5"
            opacity="0.5"
          />
        </g>
      ))}
    </svg>
  )
}
