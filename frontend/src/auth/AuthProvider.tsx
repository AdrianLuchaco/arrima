import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api } from '../api/client'
import { AuthContext, type AuthStatus, type RegisterData } from './AuthContext'
import { clearSession, onSignedOut, refreshSession, setSession, type Tokens } from './session'

const RESTORE_RETRY_MS = 3000

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [status, setStatus] = useState<AuthStatus>('checking')

  useEffect(() => {
    let cancelled = false
    let retry: ReturnType<typeof setTimeout> | undefined
    // Reopening the app: the refresh cookie (if any) restores the session without typing anything.
    // Without coverage it keeps trying: losing the connection is not signing out.
    function restore() {
      void refreshSession().then((result) => {
        if (cancelled) return
        if (result === 'offline') retry = setTimeout(restore, RESTORE_RETRY_MS)
        else setStatus(result === 'refreshed' ? 'signedIn' : 'signedOut')
      })
    }
    restore()
    const unsubscribe = onSignedOut(() => {
      queryClient.clear()
      setStatus('signedOut')
    })
    return () => {
      cancelled = true
      clearTimeout(retry)
      unsubscribe()
    }
  }, [queryClient])

  const login = useCallback(async (email: string, password: string) => {
    setSession(await api<Tokens>('/api/auth/login', { method: 'POST', body: { email, password }, authenticated: false }))
    setStatus('signedIn')
  }, [])

  const register = useCallback(async (data: RegisterData) => {
    setSession(await api<Tokens>('/api/auth/register', { method: 'POST', body: data, authenticated: false }))
    setStatus('signedIn')
  }, [])

  const logout = useCallback(async () => {
    try {
      await api('/api/auth/logout', { method: 'POST', authenticated: false })
    } finally {
      clearSession()
      queryClient.clear()
      setStatus('signedOut')
    }
  }, [queryClient])

  const value = useMemo(() => ({ status, login, register, logout }), [status, login, register, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
