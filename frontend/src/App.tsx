import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router'
import { ApiError } from './api/ApiError'
import { AuthLayout, SignedInOnly, SignedOutOnly } from './auth/routeGuards'
import { ClubProfilePage } from './pages/ClubProfilePage'
import { LoginPage } from './pages/LoginPage'
import { MeleesPage } from './pages/MeleesPage'
import { NotFoundPage } from './pages/NotFoundPage'
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

export default function App() {
  return (
    <ServerGate>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <Routes>
            <Route element={<AuthLayout />}>
              <Route element={<SignedOutOnly />}>
                <Route path="/entrar" element={<LoginPage />} />
                <Route path="/registro" element={<RegisterPage />} />
              </Route>
              <Route element={<SignedInOnly />}>
                <Route element={<AppShell />}>
                  <Route index element={<MeleesPage />} />
                  <Route path="/club" element={<ClubProfilePage />} />
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
