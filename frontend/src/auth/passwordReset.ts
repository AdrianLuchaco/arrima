import { api } from '../api/client'

/** Password recovery: ask for the e-mail with the link, then choose a new password with it. */
export const passwordReset = {
  request: (email: string) =>
    api<void>('/api/auth/password-reset/request', { method: 'POST', body: { email }, authenticated: false }),
  confirm: (token: string, password: string) =>
    api<void>('/api/auth/password-reset/confirm', { method: 'POST', body: { token, password }, authenticated: false }),
}
