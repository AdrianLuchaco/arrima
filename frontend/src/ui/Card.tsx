import type { ReactNode } from 'react'

export function Card({ title, children, className = '' }: { title?: string; children: ReactNode; className?: string }) {
  return (
    <section className={`rounded-2xl border-2 border-gravel-300 bg-white p-4 shadow-sm sm:p-6 ${className}`}>
      {title && <h2 className="mb-4 text-2xl font-bold text-steel-800">{title}</h2>}
      {children}
    </section>
  )
}
