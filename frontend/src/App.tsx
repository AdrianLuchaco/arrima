import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router'
import { ApiError } from './api/ApiError'
import { startOutbox } from './offline/outbox'
import { AuthLayout, SignedInOnly, SignedOutOnly } from './auth/routeGuards'
import { ClubProfilePage } from './pages/ClubProfilePage'
import { InternationalPage } from './international/InternationalPage'
import { MeleePage } from './melee/MeleePage'
import { LoginPage } from './pages/LoginPage'
import { MeleesPage } from './pages/MeleesPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { PrizesPage } from './prizes/PrizesPage'
import { PublicMeleePage } from './public/PublicMeleePage'
import { RegisterPage } from './pages/RegisterPage'
import { ServerGate } from './server/ServerGate'
import { AppShell } from './ui/AppShell'

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
          <Routes>
            <Route path="/m/:code" element={<PublicMeleePage />} />
            <Route element={<AuthLayout />}>
              <Route element={<SignedOutOnly />}>
                <Route path="/entrar" element={<LoginPage />} />
                <Route path="/registro" element={<RegisterPage />} />
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
        </BrowserRouter>
      </QueryClientProvider>
    </ServerGate>
  )
}
