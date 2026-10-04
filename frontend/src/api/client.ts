import { refreshSession, validAccessToken } from '../auth/session'
import { ApiError } from './ApiError'

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  /** Plain objects are sent as JSON; FormData as multipart. */
  body?: unknown
  /** Admin endpoints send the access token; public ones must not. */
  authenticated?: boolean
  signal?: AbortSignal
}

/**
 * fetch() for our API: JSON in and out, access token attached, one transparent retry after
 * renewing an expired token, and every failure turned into an ApiError.
 */
export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { authenticated = true } = options
  let response = await send(path, options, authenticated ? await validAccessToken() : null)

  if (response.status === 401 && authenticated) {
    if ((await refreshSession()) === 'refreshed') {
      response = await send(path, options, await validAccessToken())
    }
  }
  if (!response.ok) {
    throw await ApiError.from(response)
  }
  // 204, or a 202 that only says "accepted": no body to read.
  const text = await response.text()
  return (text === '' ? undefined : JSON.parse(text)) as T
}

async function send(path: string, options: RequestOptions, token: string | null): Promise<Response> {
  const headers: Record<string, string> = {}
  let body: BodyInit | undefined
  if (options.body instanceof FormData) {
    body = options.body
  } else if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(options.body)
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  try {
    return await fetch(path, { method: options.method ?? 'GET', headers, body, signal: options.signal })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw ApiError.network()
  }
}
