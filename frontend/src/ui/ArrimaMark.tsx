/** The app's mark: a steel ball with the jack beside it, as in the icon (pwa/app-icon.svg). */
export function ArrimaMark({ className = '' }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" className={className} aria-hidden="true">
      <defs>
        <radialGradient id="arrima-mark-steel" cx="35%" cy="30%" r="75%">
          <stop offset="0" stopColor="#f4f6f7" />
          <stop offset="0.45" stopColor="#9aa5ad" />
          <stop offset="1" stopColor="#2c3439" />
        </radialGradient>
      </defs>
      <circle cx="28" cy="34" r="24" fill="url(#arrima-mark-steel)" />
      <path d="M10 26c10 6 26 6 36 0M10 42c10-6 26-6 36 0" fill="none" stroke="#2c3439" strokeWidth="2" opacity=".55" />
      <circle cx="54" cy="52" r="8" fill="#e8590c" />
    </svg>
  )
}
