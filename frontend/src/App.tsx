import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { lazy, Suspense } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router'
import { ApiError } from './api/ApiError'
import { startOutbox } from './offline/outbox'
import { AuthLayout, SignedInOnly, SignedOutOnly } from './auth/routeGuards'
import { LoginPage } from './pages/LoginPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { RegisterPage } from './pages/RegisterPage'
import { ForgotPasswordPage } from './pages/ForgotPasswordPage'
import { ResetPasswordPage } from './pages/ResetPasswordPage'
import { ServerGate } from './server/ServerGate'
import { AppShell } from './ui/AppShell'
import { LoadingScreen } from './components/LoadingScreen'

// Each area downloads its own code: a player opening the QR does not wait for the admin's screens.
// The service worker keeps every piece, so the app still opens without signal.
const PublicMeleePage = lazy(() => import('./public/PublicMeleePage').then((module) => ({ default: module.PublicMeleePage })))
const MeleesPage = lazy(() => import('./pages/MeleesPage').then((module) => ({ default: module.MeleesPage })))
const ClubProfilePage = lazy(() => import('./pages/ClubProfilePage').then((module) => ({ default: module.ClubProfilePage })))
const MeleePage = lazy(() => import('./melee/MeleePage').then((module) => ({ default: module.MeleePage })))
const InternationalPage = lazy(() => import('./international/InternationalPage').then((module) => ({ default: module.InternationalPage })))
const PrizesPage = lazy(() => import('./prizes/PrizesPage').then((module) => ({ default: module.PrizesPage })))

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Retrying a 4xx never helps; network failures and 5xx get a couple of retries.
      retry: (failureCount, error) => !(error instanceof ApiError && error.status >= 400 && error.status < 500) && failureCount < 2,
      staleTime: 10_000,
    },
  },
})

// Sends whatever results were left unsent last time the app was open.
startOutbox(queryClient)

export default function App() {
  return (
    <ServerGate>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <Suspense fallback={<LoadingScreen title="" quiet />}>
          <Routes>
            <Route path="/m/:code" element={<PublicMeleePage />} />
            {/* Outside the session guards: the link from the e-mail works even with a session open. */}
            <Route path="/restablecer" element={<ResetPasswordPage />} />
            <Route element={<AuthLayout />}>
              <Route element={<SignedOutOnly />}>
                <Route path="/entrar" element={<LoginPage />} />
                <Route path="/registro" element={<RegisterPage />} />
                <Route path="/recuperar" element={<ForgotPasswordPage />} />
              </Route>
              <Route element={<SignedInOnly />}>
                <Route element={<AppShell />}>
                  <Route index element={<MeleesPage />} />
                  <Route path="/club" element={<ClubProfilePage />} />
                  <Route path="/melees/:meleeId" element={<MeleePage />} />
                  <Route path="/melees/:meleeId/internacional" element={<InternationalPage />} />
                  <Route path="/melees/:meleeId/premios" element={<PrizesPage />} />
                </Route>
              </Route>
            </Route>
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
          </Suspense>
        </BrowserRouter>
      </QueryClientProvider>
    </ServerGate>
  )
}
