import type { ReactNode } from 'react'

/** Fixed at the bottom of the screen during the matches: the countdown and the win counter. */
export function BottomBar({ children }: { children: ReactNode }) {
  return (
    <section className="fixed inset-x-0 bottom-0 z-20 border-t-4 border-jack-500 bg-steel-800 text-gravel-50 shadow-[0_-4px_12px_rgba(0,0,0,0.25)]">
      <div className="mx-auto max-w-4xl">{children}</div>
    </section>
  )
}
