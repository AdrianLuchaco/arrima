import { useTranslation } from 'react-i18next'
import { ApiError } from '../api/ApiError'

/** Spanish text for any error: the backend sends codes, never user-facing messages. */
export function useErrorText() {
  const { t, i18n } = useTranslation()
  return {
    message(error: unknown): string {
      const code = error instanceof ApiError ? error.code : 'UNKNOWN'
      const key = `errors.${code}`
      return i18n.exists(key) ? t(key as 'errors.UNKNOWN') : t('errors.UNKNOWN')
    },
    field(error: unknown, field: string): string | undefined {
      if (!(error instanceof ApiError)) return undefined
      const reason = error.fields[field]
      if (!reason) return undefined
      const key = `fieldErrors.${reason}`
      return i18n.exists(key) ? t(key as 'fieldErrors.NotBlank') : t('fieldErrors.Invalid')
    },
  }
}
