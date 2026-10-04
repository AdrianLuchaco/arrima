import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { ArrimaMark } from '../ui/ArrimaMark'

/** Frame for sign-in and registration: app name on top, one simple form below. */
export function AuthPageFrame({ title, children }: { title: string; children: ReactNode }) {
  const { t } = useTranslation()
  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col justify-center gap-6 px-4 py-8">
      <header className="text-center">
        <p className="flex items-center justify-center gap-3 text-5xl font-extrabold tracking-tight text-steel-800">
          <ArrimaMark className="size-14" />
          {t('app.name')}
        </p>
        <p className="mt-1 text-lg text-steel-600">{t('app.tagline')}</p>
      </header>
      <section className="rounded-2xl border-2 border-gravel-300 bg-white p-5 shadow-sm">
        <h1 className="mb-5 text-2xl font-bold">{title}</h1>
        {children}
      </section>
    </main>
  )
}
