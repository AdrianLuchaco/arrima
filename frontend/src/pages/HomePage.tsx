import { useTranslation } from 'react-i18next'

// Placeholder until phase 2, where this becomes the club's sign-in.
export function HomePage() {
  const { t } = useTranslation()

  return (
    <main className="mx-auto flex min-h-dvh max-w-xl flex-col items-center justify-center gap-4 px-4 text-center">
      <h1 className="text-5xl font-extrabold tracking-tight text-steel-800">{t('app.name')}</h1>
      <p className="text-xl text-steel-600">{t('app.tagline')}</p>
      <p className="mt-6 inline-flex items-center gap-2 rounded-full bg-steel-800 px-5 py-3 text-lg font-semibold text-gravel-50">
        <span aria-hidden="true" className="size-3 rounded-full bg-jack-500" />
        {t('home.serverReady')}
      </p>
    </main>
  )
}
