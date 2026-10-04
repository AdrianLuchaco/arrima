/**
 * The admin's session. The access token lives only in memory (never in localStorage, where any
 * injected script could read it). The refresh token is an HttpOnly cookie the browser sends by
 * itself to /api/auth, so reopening the app restores the session with one call.
 */

export interface Tokens {
  accessToken: string
  expiresAt: string
}

/** 'offline': no answer from the server right now (no coverage, or it is restarting). */
export type RefreshResult = 'refreshed' | 'signedOut' | 'offline'

let accessToken: string | null = null
let expiresAtMs = 0
let refreshing: Promise<RefreshResult> | null = null
const signedOutListeners = new Set<() => void>()

export function setSession(tokens: Tokens) {
  accessToken = tokens.accessToken
  expiresAtMs = Date.parse(tokens.expiresAt)
}

export function clearSession() {
  const wasSignedIn = accessToken !== null
  accessToken = null
  expiresAtMs = 0
  if (wasSignedIn) signedOutListeners.forEach((listener) => listener())
}

export function onSignedOut(listener: () => void) {
  signedOutListeners.add(listener)
  return () => {
    signedOutListeners.delete(listener)
  }
}

/** The current access token, renewed first if it is about to expire. */
export async function validAccessToken(): Promise<string | null> {
  if (accessToken && Date.now() > expiresAtMs - 30_000) {
    await refreshSession()
  }
  return accessToken
}

/**
 * Exchanges the refresh cookie for a new access token. Concurrent callers share one request:
 * two parallel refreshes would rotate the same token twice.
 */
export function refreshSession(): Promise<RefreshResult> {
  refreshing ??= doRefresh().finally(() => {
    refreshing = null
  })
  return refreshing
}

async function doRefresh(): Promise<RefreshResult> {
  let response: Response
  try {
    response = await fetch('/api/auth/refresh', { method: 'POST', cache: 'no-store' })
  } catch {
    // No connection: keep the session, the request can be retried later.
    return 'offline'
  }
  if (response.status === 401 || response.status === 403) {
    clearSession()
    return 'signedOut'
  }
  if (!response.ok) {
    // 429, or a 502/503 while the server restarts: the cookie is still good, try again later.
    return 'offline'
  }
  setSession((await response.json()) as Tokens)
  return 'refreshed'
}
