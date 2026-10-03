import { useEffect, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { LoadingScreen } from '../components/LoadingScreen'
import { waitForServer, type ServerStatus } from './waitForServer'

/** Renders its children only once the backend answers; until then, the petanque loading screen. */
export function ServerGate({ children }: { children: ReactNode }) {
  const { t } = useTranslation()
  const [status, setStatus] = useState<ServerStatus>('checking')

  useEffect(() => {
    const controller = new AbortController()
    void waitForServer({ onStatus: setStatus, signal: controller.signal })
    return () => controller.abort()
  }, [])

  switch (status) {
    case 'ready':
      return children
    case 'checking':
      return <LoadingScreen title={t('loading.checking')} quiet />
    case 'waking':
      return <LoadingScreen title={t('loading.waking.title')} detail={t('loading.waking.detail')} />
    case 'offline':
      return <LoadingScreen title={t('loading.offline.title')} detail={t('loading.offline.detail')} />
  }
}
