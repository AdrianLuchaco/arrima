import { useEffect } from 'react'

/**
 * Keeps the admin's screen on while a countdown runs: the admin's phone is the official alarm of the
 * table, and a sleeping screen would not sound. Where the browser does not support it, nothing happens.
 */
export function useWakeLock(active: boolean) {
  useEffect(() => {
    if (!active || !('wakeLock' in navigator)) return
    let lock: WakeLockSentinel | null = null
    let cancelled = false

    async function request() {
      try {
        lock = await navigator.wakeLock.request('screen')
        if (cancelled) void lock.release()
      } catch {
        // Refused (battery saver, tab in the background): the countdown still works.
      }
    }
    // The browser drops the lock when the page is hidden; it is asked again when it comes back.
    const onVisible = () => {
      if (document.visibilityState === 'visible') void request()
    }

    void request()
    document.addEventListener('visibilitychange', onVisible)
    return () => {
      cancelled = true
      document.removeEventListener('visibilitychange', onVisible)
      void lock?.release()
    }
  }, [active])
}
