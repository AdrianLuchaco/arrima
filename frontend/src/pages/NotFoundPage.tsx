import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

export function NotFoundPage() {
  const { t } = useTranslation()
  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col items-center justify-center gap-4 px-4 text-center">
      <h1 className="text-3xl font-extrabold">{t('notFound.title')}</h1>
      <Link to="/" className="text-lg font-semibold underline underline-offset-4">
        {t('notFound.back')}
      </Link>
    </main>
  )
}
