import { useEffect, useRef, useState } from 'react'

export type LiveState = 'live' | 'connecting' | 'polling'

const POLL_MS = 15_000
const DEBOUNCE_MS = 300

/**
 * Keeps a melee screen up to date: Server-Sent Events while the connection holds, and polling every
 * 15 seconds while it does not (the browser keeps trying to reconnect meanwhile). Both the admin
 * and spectators use it, so two phones of the same club also stay in sync.
 */
export function useMeleeLive(publicCode: string | undefined, onChange: () => void): LiveState {
  const [state, setState] = useState<LiveState>('connecting')
  const callback = useRef(onChange)
  useEffect(() => {
    callback.current = onChange
  }, [onChange])

  useEffect(() => {
    if (!publicCode) return
    let debounce: ReturnType<typeof setTimeout> | undefined
    let poll: ReturnType<typeof setInterval> | undefined
    // Several changes in a row (an import, a draw) become one refresh.
    const changed = () => {
      clearTimeout(debounce)
      debounce = setTimeout(() => callback.current(), DEBOUNCE_MS)
    }
    const startPolling = () => {
      setState('polling')
      poll ??= setInterval(changed, POLL_MS)
    }
    const stopPolling = () => {
      clearInterval(poll)
      poll = undefined
    }

    const source = new EventSource(`/api/public/melees/${encodeURIComponent(publicCode)}/events`)
    source.addEventListener('ready', () => {
      stopPolling()
      setState('live')
      changed() // we may have missed changes while disconnected
    })
    source.addEventListener('changed', changed)
    source.onerror = () => startPolling()

    return () => {
      source.close()
      stopPolling()
      clearTimeout(debounce)
    }
  }, [publicCode])

  return state
}
