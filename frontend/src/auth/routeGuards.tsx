import { Navigate, Outlet, useLocation } from 'react-router'
import { useTranslation } from 'react-i18next'
import { LoadingScreen } from '../components/LoadingScreen'
import { AuthProvider } from './AuthProvider'
import { useAuth } from './AuthContext'

/** Everything that needs to know about the admin session: sign-in pages and the admin area. */
export function AuthLayout() {
  return (
    <AuthProvider>
      <Outlet />
    </AuthProvider>
  )
}

export function SignedInOnly() {
  const { status } = useAuth()
  const { t } = useTranslation()
  const location = useLocation()
  if (status === 'checking') return <LoadingScreen title={t('loading.checking')} quiet />
  if (status === 'signedOut') return <Navigate to="/entrar" replace state={{ from: location.pathname }} />
  return <Outlet />
}

export function SignedOutOnly() {
  const { status } = useAuth()
  const { t } = useTranslation()
  if (status === 'checking') return <LoadingScreen title={t('loading.checking')} quiet />
  if (status === 'signedIn') return <Navigate to="/" replace />
  return <Outlet />
}
