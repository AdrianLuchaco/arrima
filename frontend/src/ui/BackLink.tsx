import type { ReactNode } from 'react'
import { Link } from 'react-router'

/** «◀ Volver…»: on every inner page, in the same place and big enough to hit with a thumb. */
export function BackLink({ to, children }: { to: string; children: ReactNode }) {
  return (
    <Link
      to={to}
      className="inline-flex min-h-12 items-center gap-2 self-start rounded-xl border-2 border-steel-400 bg-white px-4 text-lg font-bold text-steel-900 hover:bg-gravel-100 focus-visible:outline-4 focus-visible:outline-offset-2 focus-visible:outline-jack-500"
    >
      <span aria-hidden="true">◀</span>
      {children}
    </Link>
  )
}
