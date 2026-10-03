import { useTranslation } from 'react-i18next'

// Filled in phase 3: create a melee and list the club's melees.
export function MeleesPage() {
  const { t } = useTranslation()
  return <h1 className="text-3xl font-extrabold">{t('melees.title')}</h1>
}
