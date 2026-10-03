import { createContext, useContext } from 'react'

export type AuthStatus = 'checking' | 'signedIn' | 'signedOut'

export interface RegisterData {
  invitationCode: string
  clubName: string
  email: string
  password: string
}

interface AuthContextValue {
  status: AuthStatus
  login: (email: string, password: string) => Promise<void>
  register: (data: RegisterData) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>')
  return context
}
