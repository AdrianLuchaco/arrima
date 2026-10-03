import 'i18next'
import type { resources } from './index'

// Type-checks translation keys: t('loading.wakin') fails to compile.
declare module 'i18next' {
  interface CustomTypeOptions {
    resources: (typeof resources)['es']
  }
}
