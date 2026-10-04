export type ServerStatus = 'checking' | 'waking' | 'offline' | 'ready'

export interface WaitForServerOptions {
  onStatus: (status: ServerStatus) => void
  signal: AbortSignal
  fetchFn?: typeof fetch
  isOnline?: () => boolean
  /** Without an answer after this long, the user is told the server is waking up. */
  wakingNoticeMs?: number
  retryDelayMs?: number
  attemptTimeoutMs?: number
}

/**
 * Resolves once GET /api/health answers 200, or when `signal` is aborted.
 *
 * The backend runs on Render's free plan: if it ever sleeps, waking it takes a minute or two
 * (Render's spin-up plus the JVM start on 0.1 CPU). Meanwhile requests hang or Vercel's proxy
 * answers 502/504, so we keep retrying and tell the user what is happening instead of failing.
 */
export async function waitForServer({
  onStatus,
  signal,
  fetchFn = fetch,
  isOnline = () => navigator.onLine,
  wakingNoticeMs = 2000,
  retryDelayMs = 3000,
  attemptTimeoutMs = 30000,
}: WaitForServerOptions): Promise<void> {
  let current: ServerStatus = 'checking'
  const report = (status: ServerStatus) => {
    current = status
    onStatus(status)
  }
  report('checking')
  // Only for a first attempt that hangs: a failed one has already said what is going on.
  const wakingNotice = setTimeout(() => current === 'checking' && report('waking'), wakingNoticeMs)
  try {
    while (!signal.aborted) {
      if (await isServerUp(fetchFn, signal, attemptTimeoutMs)) {
        if (!signal.aborted) report('ready')
        return
      }
      if (signal.aborted) return
      report(isOnline() ? 'waking' : 'offline')
      await delay(retryDelayMs, signal)
    }
  } finally {
    clearTimeout(wakingNotice)
  }
}

async function isServerUp(fetchFn: typeof fetch, signal: AbortSignal, timeoutMs: number): Promise<boolean> {
  // A per-attempt timeout linked to the caller's signal. AbortSignal.any() would be shorter,
  // but it needs Safari 17.4 and older iPhones must keep working.
  const attempt = new AbortController()
  const abortAttempt = () => attempt.abort()
  const timeout = setTimeout(abortAttempt, timeoutMs)
  signal.addEventListener('abort', abortAttempt)
  try {
    const response = await fetchFn('/api/health', { cache: 'no-store', signal: attempt.signal })
    return response.ok
  } catch {
    return false
  } finally {
    clearTimeout(timeout)
    signal.removeEventListener('abort', abortAttempt)
  }
}

function delay(ms: number, signal: AbortSignal): Promise<void> {
  return new Promise((resolve) => {
    const timer = setTimeout(done, ms)
    signal.addEventListener('abort', done)
    function done() {
      clearTimeout(timer)
      signal.removeEventListener('abort', done)
      resolve()
    }
  })
}
