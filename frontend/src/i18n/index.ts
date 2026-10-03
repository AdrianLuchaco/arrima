import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import es from './es.json'

// Spanish only for now. Catalan will be another JSON file with the same keys.
export const resources = {
  es: { translation: es },
} as const

void i18n.use(initReactI18next).init({
  resources,
  lng: 'es',
  fallbackLng: 'es',
  // React already escapes everything it renders.
  interpolation: { escapeValue: false },
})

export default i18n
