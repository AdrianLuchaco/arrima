import { afterEach, describe, expect, it, vi } from 'vitest'
import { clearSession, refreshSession, setSession } from '../auth/session'
import { ApiError } from './ApiError'
import { api } from './client'

const json = (status: number, body: unknown) => Promise.resolve(new Response(JSON.stringify(body), { status }))
const tokens = (accessToken: string) => ({ accessToken, expiresAt: new Date(Date.now() + 15 * 60_000).toISOString() })

describe('api client', () => {
  afterEach(() => {
    clearSession()
    vi.unstubAllGlobals()
  })

  it('renews an expired access token and retries the request once', async () => {
    setSession(tokens('old-token'))
    const fetchMock = vi.fn((url: string, init?: RequestInit) => {
      if (url === '/api/auth/refresh') return json(200, tokens('new-token'))
      const auth = (init?.headers as Record<string, string> | undefined)?.Authorization
      return auth === 'Bearer new-token' ? json(200, { name: 'Club' }) : json(401, { code: 'UNAUTHENTICATED' })
    })
    vi.stubGlobal('fetch', fetchMock)

    await expect(api('/api/club')).resolves.toEqual({ name: 'Club' })
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  it('turns error responses into ApiError with the backend code and fields', async () => {
    vi.stubGlobal('fetch', vi.fn(() => json(400, { code: 'VALIDATION_FAILED', fields: { name: 'NotBlank' } })))

    const error = await api('/api/club', { authenticated: false }).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).code).toBe('VALIDATION_FAILED')
    expect((error as ApiError).fields).toEqual({ name: 'NotBlank' })
  })

  it('reports a network failure as such', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new TypeError('Failed to fetch'))))

    const error = await api('/api/club', { authenticated: false }).catch((e: unknown) => e)

    expect((error as ApiError).isNetworkError).toBe(true)
  })
})

describe('refreshSession', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('shares one request between simultaneous callers, so the token is rotated only once', async () => {
    const fetchMock = vi.fn(() => json(200, tokens('t')))
    vi.stubGlobal('fetch', fetchMock)

    const results = await Promise.all([refreshSession(), refreshSession(), refreshSession()])

    expect(results).toEqual(['refreshed', 'refreshed', 'refreshed'])
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('keeps the session when there is no connection', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new TypeError('Failed to fetch'))))

    await expect(refreshSession()).resolves.toBe('offline')
  })
})
